package net.bernerbits.dnd.donjon

import java.util.EnumSet
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

class RoomBuilder(
  private val random: Random,

  private val cellBuilder: CellBuilder,

  private val nI: Int,
  private val nJ: Int,
  roomMin: Int,
  private val roomMax: Int,
  private val roomLayout: RoomLayout
) {
  private var nRooms: Int = 0
  private var lastRoomId: Int = 0
  private val rooms = mutableListOf<MutableRoom>()
  private val connects = mutableMapOf<String, Int>()
  private val maxRow = nI * 2 - 1
  private val maxCol = nJ * 2 - 1
  private val roomBase = (roomMin + 1 / 2)
  private val roomRadix = (roomMax - roomMin) / 2 + 1

  val doorBuilder = DoorBuilder(random, cellBuilder, rooms)

  fun packRooms() {
    for (i in 0 until nI) {
      val r = (i * 2) + 1
      for (j in 0 until nJ) {
        val c = (j * 2) + 1

        if (cellBuilder.cells[r][c].attributes.contains(CellAttribute.ROOM)) continue
        if ((i == 0 || j == 0) && random.nextInt(2) == 0) continue

        val proto = RoomPrototype(i = i, j = j)
        placeRoom(proto)
      }
    }
  }

  fun scatterRooms() {
    val nRooms = allocRooms()

    repeat(nRooms) {
      placeRoom()
    }
  }

  fun setRandomRoom(proto: RoomPrototype) {
    proto.height = proto.height ?: (random.nextInt(roomRadix) + roomBase)
    proto.width = proto.width ?: (random.nextInt(roomRadix) + roomBase)
    proto.i = proto.i ?: random.nextInt(nI - proto.height!!)
    proto.j = proto.j ?: random.nextInt(nJ - proto.width!!)
  }

  fun selectRoom(proto: RoomPrototype): RoomPrototype {
    if (proto.height == null) {
      if (proto.i != null) {
        val a = max(0, nI - roomBase - proto.i!!);
        val r = max(a, roomRadix)

        proto.height = random.nextInt(r) + roomBase
      } else {
        proto.height = random.nextInt(roomRadix) + roomBase
      }
    }
    if (proto.width == null) {
      if (proto.j != null) {
        val a = max(0, nJ - roomBase - proto.j!!);
        val r = max(a, roomRadix)

        proto.width = random.nextInt(r) + roomBase
      } else {
        proto.width = random.nextInt(roomRadix) + roomBase
      }
    }
    if (proto.i == null) {
      proto.i = random.nextInt(nI - proto.height!!)
    }
    if (proto.j == null) {
      proto.j = random.nextInt(nJ - proto.width!!)
    }
    return proto
  }

  fun placeRoom(proto: RoomPrototype? = null) {
    if (nRooms == 999) return

    val room = proto?.let { selectRoom(it) } ?: RoomPrototype().apply { setRandomRoom(this) }

    val r1 = (room.i!! * 2) + 1
    val c1 = (room.j!! * 2) + 1
    val r2 = ((room.i!! + room.height!!) * 2) - 1
    val c2 = ((room.j!! + room.width!!) * 2) - 1

    if (r1 < 1 || r2 > maxRow || c1 < 1 || c2 > maxCol) return

    if (checkRoomPlacement(r1, c1, r2, c2).isNotEmpty()) return

    nRooms += 1
    val roomId = nRooms
    lastRoomId = roomId

    for (r in r1..r2) {
      for (c in c1..c2) {
        with(cellBuilder.cells[r][c]) {
          attributes.add(CellAttribute.ROOM)
          this.roomId = roomId
          if (attributes.contains(CellAttribute.ENTRANCE)) {
            attributes.removeAll(ESPACE)
          }
          attributes.remove(CellAttribute.PERIMETER)
        }
      }
    }

    val roomOrEntrance = EnumSet.of(CellAttribute.ROOM, CellAttribute.ENTRANCE)
    for (r in (r1 - 1)..(r2 + 1)) {
      if (!cellBuilder.cells[r][c1 - 1].attributes.containsAny(roomOrEntrance)) {
        cellBuilder.cells[r][c1 - 1].attributes.add(CellAttribute.PERIMETER)
      }
      if (!cellBuilder.cells[r][c2 + 1].attributes.containsAny(roomOrEntrance)) {
        cellBuilder.cells[r][c2 + 1].attributes.add(CellAttribute.PERIMETER)
      }
    }
    for (c in (c1 - 1)..(c2 + 1)) {
      if (!cellBuilder.cells[r1 - 1][c].attributes.containsAny(roomOrEntrance)) {
        cellBuilder.cells[r1 - 1][c].attributes.add(CellAttribute.PERIMETER)
      }
      if (!cellBuilder.cells[r2 + 1][c].attributes.containsAny(roomOrEntrance)) {
        cellBuilder.cells[r2 + 1][c].attributes.add(CellAttribute.PERIMETER)
      }
    }

    val roomData = MutableRoom(roomId, r1, c1, r1, r2, c1, c2, (r2 - r1 + 1) * 10, (c2 - c1 + 1) * 10, ((r2 - r1 + 1) * 10) * ((c2 - c1 + 1) * 10))
    this.rooms.add(roomData)
  }

  fun allocRooms(): Int {
    val dungeonArea = cellBuilder.nCols * cellBuilder.nRows
    val roomArea = roomMax * roomMax
    return dungeonArea / roomArea
  }

  fun checkRoomPlacement(r1: Int, c1: Int, r2: Int, c2: Int): Map<Int, Int> {
    val hit = mutableMapOf<Int, Int>()
    for (r in r1..r2) {
      for (c in c1..c2) {
        if (cellBuilder.cells[r][c].attributes.contains(CellAttribute.BLOCKED)) {
          return mapOf()
        }
        if (cellBuilder.cells[r][c].attributes.contains(CellAttribute.ROOM)) {
          val id = cellBuilder.cells[r][c].roomId
          if (id != 0) {
            hit[id] = hit.getOrDefault(id, 0) + 1
          }
        }
      }
    }
    return hit
  }

  fun openRooms() {
    for (id in 1..nRooms) {
      val room = rooms[id - 1]
      openRoom(room)
    }
    connects.clear()
  }

  fun openRoom(room: MutableRoom) {
    val sills = doorBuilder.doorSills(room)
    if (sills.isEmpty()) return

    val nOpens = allocOpens(room)

    for(i in 0 ..< nOpens) {
      val sill = sills.randomOrNull() ?: break
      sills.remove(sill)

      val doorR = sill.doorR
      val doorC = sill.doorC
      if (cellBuilder.cells[doorR][doorC].attributes.any { it in DOORSPACE }) continue

      val outId = sill.outId
      if (outId != null) {
        val connectKey = listOf(room.id, outId).sorted().joinToString(",")
        if (connects.containsKey(connectKey)) continue
        connects[connectKey] = 1
      }

      val openR = sill.sillR
      val openC = sill.sillC
      val openDir = sill.dir

      for (x in 0 until 3) {
        val r = openR + openDir.delta.first * x
        val c = openC + openDir.delta.second * x
        with(cellBuilder.cells[r][c].attributes) {
          remove(CellAttribute.PERIMETER)
          add(CellAttribute.ENTRANCE)
        }
      }

      val doorType = doorBuilder.doorType()
      val door = MutableDoor(row = doorR, col = doorC, type = doorType)
      if (outId != null) {
        door.outId = outId
      }
      room.doors.getOrPut(openDir) { mutableListOf() }.add(door)
    }
  }

  fun allocOpens(room: MutableRoom): Int {
    val roomH = ((room.south - room.north) / 2) + 1
    val roomW = ((room.east - room.west) / 2) + 1
    val linearRoomMeasure = sqrt((roomW * roomH).toDouble()).toInt()
    return linearRoomMeasure + random.nextInt(linearRoomMeasure)
  }


  fun placeRooms() {
    when (roomLayout) {
      RoomLayout.PACKED -> packRooms()
      RoomLayout.SCATTERED -> scatterRooms()
    }
  }

  fun labelRooms() {
    for (room in rooms) {
      val label = room.id.toString()
      val labelR = (room.north + room.south) / 2
      val labelC = (room.west + room.east - label.length) / 2 + 1

      for ((index, char) in label.withIndex()) {
        cellBuilder.cells[labelR][labelC + index].label = char.toString()
      }
    }
  }

  fun buildRooms(): List<Room> {
    return rooms.map { it.toRoom() }
  }
}

class MutableRoom(
  var id: Int,
  var row: Int,
  var col: Int,
  var north: Int,
  var south: Int,
  var west: Int,
  var east: Int,
  var height: Int,
  var width: Int,
  var area: Int,
  var doors: MutableMap<Direction, MutableList<MutableDoor>> = mutableMapOf()
) {
  fun toRoom() = Room(id, row, col, north, south, west, east, height, width, area, doors.mapValues { (_,l) -> l.map{ it.toDoor() }})
}

data class Room(
  var id: Int,
  var row: Int,
  var col: Int,
  var north: Int,
  var south: Int,
  var west: Int,
  var east: Int,
  var height: Int,
  var width: Int,
  var area: Int,
  var doors: Map<Direction, List<Door>>
)


data class RoomPrototype(
  var i: Int? = null,
  var j: Int? = null,
  var width: Int? = null,
  var height: Int? = null
)

enum class RoomLayout {
  PACKED,
  SCATTERED;
}
