package net.bernerbits.dnd.donjon

import kotlin.random.Random

class DoorBuilder(
  val random: Random,
  val cellBuilder: CellBuilder,

  val rooms: List<MutableRoom>,
) {

  val doors = mutableListOf<MutableDoor>()

  fun fixDoors() {
    val fixed = Array(cellBuilder.nRows + 1) { BooleanArray(cellBuilder.nCols + 1) }

    for (room in this.rooms) {
      for (dir in room.doors.keys.sorted()) {
        val shinyDoors = mutableListOf<MutableDoor>()

        room.doors[dir]?.forEach { door ->
          val doorR = door.row
          val doorC = door.col

          if (cellBuilder.cells[doorR][doorC].attributes.containsAny(OPENSPACE) && !fixed[doorR][doorC]) {
            door.outId?.let {
              val outDir = dir.opposite
              this.rooms[it - 1].doors.getOrPut(outDir) { mutableListOf() }.add(door)
            }
            shinyDoors.add(door)
            fixed[doorR][doorC] = true
          }
        }

        if (shinyDoors.isNotEmpty()) {
          room.doors[dir] = shinyDoors
          doors.addAll(shinyDoors)
        } else {
          room.doors.remove(dir)
        }
      }
    }
  }

  fun doorType(): DoorType {
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

  fun doorSills(room: MutableRoom): MutableList<Sill> {
    val list = mutableListOf<Sill>()

    if (room.north >= 3) {
      for (c in room.west..room.east step 2) {
        checkSill(room, room.north, c, Direction.NORTH)?.let { list.add(it) }
      }
    }
    if (room.south <= cellBuilder.nRows - 3) {
      for (c in room.west..room.east step 2) {
        checkSill(room, room.south, c, Direction.SOUTH)?.let { list.add(it) }
      }
    }
    if (room.west >= 3) {
      for (r in room.north..room.south step 2) {
        checkSill(room, r, room.west, Direction.WEST)?.let { list.add(it) }
      }
    }
    if (room.east <= cellBuilder.nCols - 3) {
      for (r in room.north..room.south step 2) {
        checkSill(room, r, room.east, Direction.EAST)?.let { list.add(it) }
      }
    }
    return mutableListOf(*list.shuffled().toTypedArray())
  }

  fun checkSill(room: MutableRoom, sillR: Int, sillC: Int, dir: Direction): Sill? {
    val doorR = sillR + dir.delta.first
    val doorC = sillC + dir.delta.second
    val doorCell = cellBuilder.cells[doorR][doorC]
    if (doorCell.attributes.contains(CellAttribute.PERIMETER).not() || doorCell.attributes.containsAny(BLOCK_DOOR)) return null

    val outR = doorR + dir.delta.first
    val outC = doorC + dir.delta.second
    val outCell = cellBuilder.cells[outR][outC]
    if (outCell.attributes.contains(CellAttribute.BLOCKED)) return null

    val outId: Int? =
      if (outCell.attributes.contains(CellAttribute.ROOM)) {
        if (outCell.roomId == room.id) return null
        outCell.roomId
      } else null

    return Sill(sillR, sillC, dir, doorR, doorC, outId)
  }

  fun buildDoors(): List<Door> {
    return doors.map { it.toDoor() }
  }
}

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

class MutableDoor(var row: Int, var col: Int, var outId: Int? = null, var type: DoorType) {
  fun toDoor() =
    Door(row, col, outId, type)
}

data class Door(val row: Int, val col: Int, val outId: Int?, val type: DoorType)

data class Sill(
  val sillR: Int,
  val sillC: Int,
  val dir: Direction,
  val doorR: Int,
  val doorC: Int,
  val outId: Int? = null
)

