package net.bernerbits.dnd.donjon

import java.util.EnumSet
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

class Dungeon(
  var nRows: Int,
  var nCols: Int,
  var dungeonLayout: DungeonLayout?,
  var roomMin: Int,
  var roomMax: Int,
  var roomLayout: RoomLayout,
  var corridorLayout: CorridorLayout,
  var removeDeadends: Int,
  var addStairs: Int,
  var mapStyle: MapStyle,
  var cellSize: Int,
  var seed: Long,
  var grid: Grid?
) {
  private var nI: Int = 0
  private var nJ: Int = 0
  private var maxRow: Int = 0
  private var maxCol: Int = 0
  private var nRooms: Int = 0
  private var lastRoomId: Int = 0
  private var roomBase: Int = 0
  private var roomRadix: Int = 0

  var cells: Array<Array<Cell>> = emptyArray()
  var rooms: MutableList<Room> = mutableListOf()
  var connects: MutableMap<String, Int> = mutableMapOf()
  var stairs: MutableList<Stair> = mutableListOf()
  var doors: MutableList<Door> = mutableListOf()

  val random = Random(seed)
  
  fun initialize() {
    nI = nRows / 2
    nJ = nCols / 2
    nRows = nI * 2
    nCols = nI * 2
    maxRow = nI * 2 - 1
    maxCol = nJ * 2 - 1
    roomBase = (roomMin + 1) / 2
    roomRadix = (roomMax - roomMin) / 2 + 1
    cells = Array(nRows + 1) { Array(nCols + 1) { Cell() } }
  }

  fun initializeCells() {
    val mask = dungeonLayout?.pattern
    if (mask != null) {
      maskCells(mask)
    } else if (dungeonLayout == DungeonLayout.ROUND) {
      roundMask()
    }
  }

  fun maskCells(mask: List<List<Int>>) {
    val rX = mask.size.toDouble() / (nRows + 1)
    val cX = mask[0].size.toDouble() / (nCols + 1)

    for (r in 0 .. nRows) {
      for (c in 0 .. nCols) {
        if (mask.getOrNull((r*rX).toInt())?.getOrNull((c*cX).toInt()) == 0) {
          cells[r][c].attributes.add(CellAttribute.BLOCKED)
        }
      }
    }
  }

  fun roundMask() {
    val centerR = nRows / 2
    val centerC = nCols / 2

    for (r in 0 .. nRows) {
      for (c in 0 .. nCols) {
        val dR = (r - centerR).toDouble()
        val dC = (c - centerC).toDouble()
        val distance = sqrt(dR * dR + dC * dC).toInt()
        if (distance > centerC) {
          cells[r][c].attributes.add(CellAttribute.BLOCKED)
        }
      }
    }
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
        cells[labelR][labelC + index].label = char.toString()
      }
    }
  }

  fun corridors() {
    for (i in 1 until nI) {
      val r = (i * 2) + 1
      for (j in 1 until nJ) {
        val c = (j * 2) + 1

        if (!cells[r][c].attributes.contains(CellAttribute.CORRIDOR)) {
          tunnel(i, j)
        }
      }
    }
  }

  fun cleanDungeon() {
    if (removeDeadends > 0) {
      collapseTunnels(removeDeadends)
    }
    fixDoors()
    emptyBlocks()
  }

  private fun packRooms() {
    for (i in 0 until nI) {
      val r = (i * 2) + 1
      for (j in 0 until nJ) {
        val c = (j * 2) + 1

        if (cells[r][c].attributes.contains(CellAttribute.ROOM)) continue
        if ((i == 0 || j == 0) && random.nextInt(2) == 0) continue

        val proto = RoomPrototype(i = i, j = j)
        placeRoom(proto)
      }
    }
  }

  private fun scatterRooms() {
    val nRooms = allocRooms()

    repeat(nRooms) {
      placeRoom()
    }
  }

  private fun setRandomRoom(proto: RoomPrototype) {
    proto.height = proto.height ?: (random.nextInt(roomRadix) + roomBase)
    proto.width = proto.width ?: (random.nextInt(roomRadix) + roomBase)
    proto.i = proto.i ?: random.nextInt(nI - proto.height!!)
    proto.j = proto.j ?: random.nextInt(nJ - proto.width!!)
  }

  private fun selectRoom(proto: RoomPrototype): RoomPrototype {
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

  private fun placeRoom(proto: RoomPrototype? = null) {
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
        cells[r][c] = cells[r][c].apply {
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
      if (!cells[r][c1 - 1].attributes.containsAny(roomOrEntrance)) {
        cells[r][c1 - 1].attributes.add(CellAttribute.PERIMETER)
      }
      if (!cells[r][c2 + 1].attributes.containsAny(roomOrEntrance)) {
        cells[r][c2 + 1].attributes.add(CellAttribute.PERIMETER)
      }
    }
    for (c in (c1 - 1)..(c2 + 1)) {
      if (!cells[r1 - 1][c].attributes.containsAny(roomOrEntrance)) {
        cells[r1 - 1][c].attributes.add(CellAttribute.PERIMETER)
      }
      if (!cells[r2 + 1][c].attributes.containsAny(roomOrEntrance)) {
        cells[r2 + 1][c].attributes.add(CellAttribute.PERIMETER)
      }
    }

    val roomData = Room(roomId, r1, c1, r1, r2, c1, c2, (r2 - r1 + 1) * 10, (c2 - c1 + 1) * 10, ((r2 - r1 + 1) * 10) * ((c2 - c1 + 1) * 10))
    this.rooms.add(roomData)
  }

  private fun allocRooms(): Int {
    val dungeonArea = nCols * nRows
    val roomArea = roomMax * roomMax
    return dungeonArea / roomArea
  }

  private fun checkRoomPlacement(r1: Int, c1: Int, r2: Int, c2: Int): Map<Int, Int> {
    val hit = mutableMapOf<Int, Int>()
    for (r in r1..r2) {
      for (c in c1..c2) {
        if (cells[r][c].attributes.contains(CellAttribute.BLOCKED)) {
          return mapOf()
        }
        if (cells[r][c].attributes.contains(CellAttribute.ROOM)) {
          val id = cells[r][c].roomId
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

  private fun openRoom(room: Room) {
    val sills = doorSills(room)
    if (sills.isEmpty()) return

    val nOpens = allocOpens(room)

    for(i in 0 ..< nOpens) {
      val sill = sills.randomOrNull() ?: break
      sills.remove(sill)

      val doorR = sill.doorR
      val doorC = sill.doorC
      if (cells[doorR][doorC].attributes.any { it in DOORSPACE }) continue

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
        cells[r][c].attributes.remove(CellAttribute.PERIMETER)
        cells[r][c].attributes.remove(CellAttribute.ENTRANCE)
      }

      val doorType = doorType()
      val door = Door(row = doorR, col = doorC, type = doorType)
      if (outId != null) {
        door.outId = outId
      }
      room.door.getOrPut(openDir) { mutableListOf() }.add(door)
    }
  }

  private fun doorType(): DoorType {
    val i = random.nextInt(110)
    return when {
      i < 15 -> DoorType.ARCH
      i < 60 -> DoorType.OPEN
      i < 75 -> DoorType.LOCK
      i < 90 -> DoorType.TRAP
      i < 100 -> DoorType.SECRET
      else -> DoorType.PORTC
    }
  }

  private fun allocOpens(room: Room): Int {
    val roomH = ((room.south - room.north) / 2) + 1
    val roomW = ((room.east - room.west) / 2) + 1
    val linearRoomMeasure = kotlin.math.sqrt((roomW * roomH).toDouble()).toInt()
    return linearRoomMeasure + kotlin.random.Random(seed).nextInt(linearRoomMeasure)
  }

  private fun doorSills(room: Room): MutableList<Sill> {
    val list = mutableListOf<Sill>()

    if (room.north >= 3) {
      for (c in room.west..room.east step 2) {
        checkSill(room, room.north, c, Direction.NORTH)?.let { list.add(it) }
      }
    }
    if (room.south <= nRows - 3) {
      for (c in room.west..room.east step 2) {
        checkSill(room, room.south, c, Direction.SOUTH)?.let { list.add(it) }
      }
    }
    if (room.west >= 3) {
      for (r in room.north..room.south step 2) {
        checkSill(room, r, room.west, Direction.WEST)?.let { list.add(it) }
      }
    }
    if (room.east <= nCols - 3) {
      for (r in room.north..room.south step 2) {
        checkSill(room, r, room.east, Direction.EAST)?.let { list.add(it) }
      }
    }
    return mutableListOf(*list.shuffled().toTypedArray())
  }

  private fun checkSill(room: Room, sillR: Int, sillC: Int, dir: Direction): Sill? {
    val doorR = sillR + dir.delta.first
    val doorC = sillC + dir.delta.second
    val doorCell = cells[doorR][doorC]
    if (doorCell.attributes.contains(CellAttribute.PERIMETER).not() || doorCell.attributes.containsAny(BLOCK_DOOR)) return null

    val outR = doorR + dir.delta.first
    val outC = doorC + dir.delta.second
    val outCell = cells[outR][outC]
    if (outCell.attributes.contains(CellAttribute.BLOCKED)) return null

    val outId: Int? =
      if (outCell.attributes.contains(CellAttribute.ROOM)) {
        if (outCell.roomId == room.id) return null
        outCell.roomId
      } else null

    return Sill(sillR, sillC, dir, doorR, doorC, outId)
  }

  private fun tunnel(i: Int, j: Int, lastDir: Direction? = null) {
    val directions = tunnelDirs(lastDir)

    for (dir in directions) {
      if (openTunnel(i, j, dir)) {
        val nextI = i + dir.delta.first
        val nextJ = j + dir.delta.second
        tunnel(nextI, nextJ, dir)
      }
    }
  }

  private fun tunnelDirs(lastDir: Direction?): List<Direction> {
    val p = corridorLayout.probability
    val dirs = Direction.entries.shuffled()

    return if (lastDir != null && random.nextInt(100) < p) {
      listOf(lastDir) + dirs.filter { it != lastDir }
    } else {
      dirs
    }
  }

  private fun openTunnel(i: Int, j: Int, dir: Direction): Boolean {
    val thisR = (i * 2) + 1
    val thisC = (j * 2) + 1
    val nextR = ((i + dir.delta.first) * 2) + 1
    val nextC = ((j + dir.delta.second) * 2) + 1
    val midR = (thisR + nextR) / 2
    val midC = (thisC + nextC) / 2

    return if (checkTunnelPlacement(midR, midC, nextR, nextC)) {
      delveTunnel(thisR, thisC, nextR, nextC)
    } else false
  }

  private fun checkTunnelPlacement(midR: Int, midC: Int, nextR: Int, nextC: Int): Boolean {
    if (nextR < 0 || nextR > nRows || nextC < 0 || nextC > nCols) return false

    val rRange = minOf(midR, nextR)..maxOf(midR, nextR)
    val cRange = minOf(midC, nextC)..maxOf(midC, nextC)

    for (r in rRange) {
      for (c in cRange) {
        if (cells[r][c].attributes.any { it in BLOCK_CORR }) return false
      }
    }
    return true
  }

  private fun delveTunnel(thisR: Int, thisC: Int, nextR: Int, nextC: Int): Boolean {
    val rRange = minOf(thisR, nextR)..maxOf(thisR, nextR)
    val cRange = minOf(thisC, nextC)..maxOf(thisC, nextC)

    for (r in rRange) {
      for (c in cRange) {
        cells[r][c].attributes.remove(CellAttribute.ENTRANCE)
        cells[r][c].attributes.add(CellAttribute.CORRIDOR)
      }
    }
    return true
  }

  private fun collapseTunnels(p: Int) {
    if (p == 0) return
    val removeAll = p == 100

    for (i in 0 until nI) {
      val r = (i * 2) + 1
      for (j in 0 until nJ) {
        val c = (j * 2) + 1

        if (cells[r][c].attributes.containsAny(OPENSPACE) && !cells[r][c].attributes.containsAny(STAIRS)) {
          if (removeAll || random.nextInt(100) < p) {
            collapse(r, c)
          }
        }
      }
    }
  }

  private fun collapse(r: Int, c: Int) {
    if (!cells[r][c].attributes.containsAny(OPENSPACE)) return

    for (dir in Direction.entries) {
      val check = dir.closeEnd
      if (checkTunnel(cells, r, c, check)) {
        check.close.forEach { (dr, dc) ->
          cells[r + dr][c + dc] = Cell()
        }
        check.recurse.let { (dr, dc) -> collapse(r + dr, c + dc) }
      }
    }
  }

  private fun checkTunnel(cell: Array<Array<Cell>>, r: Int, c: Int, check: TunnelCheck): Boolean {
    return check.corridor.all { (dr, dc) -> cell[r + dr][c + dc].attributes == EnumSet.of(CellAttribute.CORRIDOR) } &&
      check.walled.all { (dr, dc) -> !(cell.getOrNull(r + dr)?.getOrNull(c + dc)?.attributes?.containsAny(OPENSPACE) ?: true) }
  }

  private fun fixDoors() {
    val fixed = Array(nRows + 1) { BooleanArray(nCols + 1) }

    for (room in this.rooms) {
      for (dir in room.door.keys.sorted()) {
        val shinyDoors = mutableListOf<Door>()

        room.door[dir]?.forEach { door ->
          val doorR = door.row
          val doorC = door.col

          if (cells[doorR][doorC].attributes.containsAny(OPENSPACE) && !fixed[doorR][doorC]) {
            door.outId?.let {
              val outDir = dir.opposite
              this.rooms[it - 1].door.getOrPut(outDir) { mutableListOf() }.add(door)
            }
            shinyDoors.add(door)
            fixed[doorR][doorC] = true
          }
        }

        if (shinyDoors.isNotEmpty()) {
          room.door[dir] = shinyDoors
          doors.addAll(shinyDoors)
        } else {
          room.door.remove(dir)
        }
      }
    }
  }

  private fun emptyBlocks() {
    for (r in 0..nRows) {
      for (c in 0..nCols) {
        if (cells[r][c].attributes.contains(CellAttribute.BLOCKED)) {
          cells[r][c] = Cell()
        }
      }
    }
  }

  fun placeStairs() {
    if (addStairs > 0) {
      val list = stairEnds()
      if (list.isEmpty()) return

      repeat(addStairs) { i ->
        val stair = list.randomOrNull(random) ?: return@repeat
        list.remove(stair)

        val r = stair.row
        val c = stair.col
        val type = if (i < 2) i else random.nextInt(2)

        if (type == 0) {
          cells[r][c].attributes.add(CellAttribute.STAIR_DN)
          cells[r][c].label = "d"
          stair.key = StairKey.DOWN
        } else {
          cells[r][c].attributes.add(CellAttribute.STAIR_UP)
          cells[r][c].label = "u"
          stair.key = StairKey.UP
        }
        this.stairs.add(stair)
      }
    }
  }

  private fun stairEnds(): MutableList<Stair> {
    val list = mutableListOf<Stair>()

    for (i in 0 until nI) {
      val r = (i * 2) + 1
      for (j in 0 until nJ) {
        val c = (j * 2) + 1

        if (cells[r][c].attributes == EnumSet.of(CellAttribute.CORRIDOR)) {
          for (dir in Direction.entries) {
            if (checkTunnel(cells, r, c, dir.stairEnd)) {
              val end = Stair(r, c).apply {
                val next = dir.stairEnd.next
                nextRow = row + next.first
                nextCol = col + next.second
              }
              list.add(end)
              break
            }
          }
        }
      }
    }
    return list
  }
}

class Room(
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
  var door: MutableMap<Direction, MutableList<Door>> = mutableMapOf()
)

enum class DoorType(
  val arch: Boolean,
  val door: Boolean,
  val lock: Boolean,
  val trap: Boolean,
  val secret: Boolean,
  val portc: Boolean,
  val wall: Boolean,
  val key: String,
  val type: String
) {
  ARCH(arch = true, door = false, lock = false, trap = false, secret = false, portc = false, wall = false, "arch", "Archway"),
  OPEN(arch = true, door = true, lock = false, trap = false, secret = false, portc = false, wall = false, "open", "Unlocked Door"),
  LOCK(arch = true, door = true, lock = true, trap = false, secret = false, portc = false, wall = false, "lock", "Locked Door"),
  TRAP(arch = true, door = true, lock = false, trap = true, secret = false, portc = false, wall = false, "trap", "Trapped Door"),
  SECRET(arch = true, door = false, lock = false, trap = false, secret = true, portc = false, wall = true, "secret", "Secret Door"),
  PORTC(arch = true, door = false, lock = false, trap = false, secret = false, portc = true, wall = false, "portc", "Portcullis")
}

enum class StairKey { UP, DOWN }

class Stair(var row: Int, var col: Int, var nextRow: Int = 0, var nextCol: Int = 0, var key: StairKey? = null)

class Door(var row: Int, var col: Int, var outId: Int? = null, var type: DoorType)

data class RoomPrototype(var i: Int? = null, var j: Int? = null, var width: Int? = null, var height: Int? = null)

interface TunnelCheck {
  val corridor: List<Pair<Int, Int>>
  val walled: List<Pair<Int, Int>>
  val close: List<Pair<Int, Int>>
}

data class CloseEnd(
  override val corridor: List<Pair<Int, Int>>,
  override val walled: List<Pair<Int, Int>>,
  override val close: List<Pair<Int, Int>>,
  val recurse: Pair<Int, Int>
): TunnelCheck

data class StairEnd(
  override val corridor: List<Pair<Int, Int>>,
  override val walled: List<Pair<Int, Int>>,
  override val close: List<Pair<Int, Int>>,
  val next: Pair<Int, Int>
): TunnelCheck

enum class Direction(
  val delta: Pair<Int, Int>,
  val closeEnd: CloseEnd,
  val stairEnd: StairEnd
) {
  NORTH(
    delta = Pair(-1, 0),
    closeEnd = CloseEnd(
      corridor = listOf(Pair(0, 0)),
      walled = listOf(Pair(0, -1), Pair(1, -1), Pair(1, 0), Pair(1, 1), Pair(0, 1)),
      close = listOf(Pair(0, 0)),
      recurse = Pair(-1, 0)
    ),
    stairEnd = StairEnd(
      corridor = listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0)),
      walled = listOf(Pair(1, -1), Pair(0, -1), Pair(-1, -1), Pair(-1, 0), Pair(-1, 1), Pair(0, 1), Pair(1, 1)),
      close = emptyList(),
      next = Pair(1, 0)
    )
  ),
  SOUTH(
    delta = Pair(1, 0),
    closeEnd = CloseEnd(
      corridor = listOf(Pair(0, 0)),
      walled = listOf(Pair(0, -1), Pair(-1, -1), Pair(-1, 0), Pair(-1, 1), Pair(0, 1)),
      close = listOf(Pair(0, 0)),
      recurse = Pair(1, 0)
    ),
    stairEnd = StairEnd(
      corridor = listOf(Pair(0, 0), Pair(-1, 0), Pair(-2, 0)),
      walled = listOf(Pair(-1, -1), Pair(0, -1), Pair(1, -1), Pair(1, 0), Pair(1, 1), Pair(0, 1), Pair(-1, 1)),
      close = emptyList(),
      next = Pair(-1, 0)
    )
  ),
  WEST(
    delta = Pair(0, -1),
    closeEnd = CloseEnd(
      corridor = listOf(Pair(0, 0)),
      walled = listOf(Pair(-1, 0), Pair(-1, 1), Pair(0, 1), Pair(1, 1), Pair(1, 0)),
      close = listOf(Pair(0, 0)),
      recurse = Pair(0, -1)
    ),
    stairEnd = StairEnd(
      corridor = listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2)),
      walled = listOf(Pair(-1, 1), Pair(-1, 0), Pair(-1, -1), Pair(0, -1), Pair(1, -1), Pair(1, 0), Pair(1, 1)),
      close = emptyList(),
      next = Pair(0, 1)
    )
  ),
  EAST(
    delta = Pair(0, 1),
    closeEnd = CloseEnd(
      corridor = listOf(Pair(0, 0)),
      walled = listOf(Pair(-1, 0), Pair(-1, -1), Pair(0, -1), Pair(1, -1), Pair(1, 0)),
      close = listOf(Pair(0, 0)),
      recurse = Pair(0, 1)
    ),
    stairEnd = StairEnd(
      corridor = listOf(Pair(0, 0), Pair(0, -1), Pair(0, -2)),
      walled = listOf(Pair(-1, -1), Pair(-1, 0), Pair(-1, 1), Pair(0, 1), Pair(1, 1), Pair(1, 0), Pair(1, -1)),
      close = emptyList(),
      next = Pair(0, -1)
    )
  );

  val opposite: Direction
    get() = when (this) {
      NORTH -> SOUTH
      SOUTH -> NORTH
      WEST -> EAST
      EAST -> WEST
    }

  companion object {
    fun fromString(name: String): Direction? {
      return values().find { it.name.equals(name, ignoreCase = true) }
    }
  }
}

enum class CellAttribute() {
  BLOCKED,
  ROOM,
  CORRIDOR,
  PERIMETER,
  ENTRANCE,
  ARCH,
  DOOR,
  LOCKED,
  TRAPPED,
  SECRET,
  PORTC,
  STAIR_DN,
  STAIR_UP,
  LABEL
}

val OPENSPACE = EnumSet.of(CellAttribute.ROOM, CellAttribute.CORRIDOR)
val DOORSPACE = EnumSet.of(
  CellAttribute.ARCH,
  CellAttribute.DOOR,
  CellAttribute.LOCKED,
  CellAttribute.TRAPPED,
  CellAttribute.SECRET,
  CellAttribute.PORTC
)
val ESPACE = EnumSet.of(
  CellAttribute.ENTRANCE,
  CellAttribute.ARCH,
  CellAttribute.DOOR,
  CellAttribute.LOCKED,
  CellAttribute.TRAPPED,
  CellAttribute.SECRET,
  CellAttribute.PORTC,
  CellAttribute.LABEL
)
val STAIRS = EnumSet.of(CellAttribute.STAIR_DN, CellAttribute.STAIR_UP)

val BLOCK_CORR = EnumSet.of(CellAttribute.BLOCKED, CellAttribute.PERIMETER, CellAttribute.CORRIDOR)
val BLOCK_DOOR = EnumSet.of(
  CellAttribute.BLOCKED,
  CellAttribute.ARCH,
  CellAttribute.DOOR,
  CellAttribute.LOCKED,
  CellAttribute.TRAPPED,
  CellAttribute.SECRET,
  CellAttribute.PORTC
)

fun EnumSet<CellAttribute>.containsAny(other: EnumSet<CellAttribute>): Boolean {
  return this.intersect(other).isNotEmpty()
}

data class Cell(
  val attributes: EnumSet<CellAttribute> = EnumSet.noneOf(CellAttribute::class.java),
  var roomId: Int = 0,
  var label: String? = null
)

enum class MapStyle(val palette: Palette) {
  STANDARD(Palette.defaultStandard());
}

enum class DungeonLayout(val pattern: List<List<Int>>) {
  BOX(listOf(listOf(1, 1, 1), listOf(1, 0, 1), listOf(1, 1, 1))),
  CROSS(listOf(listOf(0, 1, 0), listOf(1, 1, 1), listOf(0, 1, 0))),
  ROUND(emptyList());
}

enum class RoomLayout {
  PACKED, SCATTERED;
}

enum class CorridorLayout(val probability: Int) {
  LABYRINTH(0),
  BENT(50),
  STRAIGHT(100);
}

enum class Grid {
  SQUARE,
  HEX
}

data class Sill(
  val sillR: Int,
  val sillC: Int,
  val dir: Direction,
  val doorR: Int,
  val doorC: Int,
  val outId: Int? = null
)
