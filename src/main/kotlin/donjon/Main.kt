package net.bernerbits.dnd.donjon

import java.security.SecureRandom

fun main() {
  val dungeon = generateDungeon()
  imageDungeon(dungeon)
}

fun generateDungeon(): Dungeon {
  val dungeon = Dungeon(
    nRows = 79,
    nCols = 79,
    dungeonLayout = null,
    roomMin = 3,
    roomMax = 12,
    roomLayout = RoomLayout.PACKED,
    corridorLayout = CorridorLayout.BENT,
    removeDeadends = 20,
    addStairs = 2,
    mapStyle = MapStyle.STANDARD,
    cellSize = 12,
    grid = Grid.SQUARE,
    seed = SecureRandom.getInstanceStrong().nextLong()
  )

  dungeon.initialize()
  dungeon.initializeCells()
  dungeon.placeRooms()
  dungeon.openRooms()
  dungeon.labelRooms()
  dungeon.corridors()
  dungeon.placeStairs()
  dungeon.cleanDungeon()

  return dungeon
}

