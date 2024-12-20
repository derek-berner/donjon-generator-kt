package net.bernerbits.dnd.donjon

import java.security.SecureRandom

fun main() {
  val dungeon = generateDungeon()
  val dungeonStyle = DungeonStyle(
    mapStyle = MapStyle.STANDARD,
    cellSize = 12,
    grid = Grid.SQUARE,
  )
  imageDungeon(dungeon, dungeonStyle)
}

fun generateDungeon(): Dungeon {
  val dungeon = DungeonBuilder(
    nRows = 79,
    nCols = 79,
    dungeonLayout = null,
    roomMin = 3,
    roomMax = 12,
    roomLayout = RoomLayout.PACKED,
    corridorLayout = CorridorLayout.BENT,
    removeDeadends = 20,
    addStairs = 2,
    seed = SecureRandom.getInstanceStrong().nextLong()
  )
  dungeon.generate()
  return dungeon.buildDungeon()
}

