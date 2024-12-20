package net.bernerbits.dnd.donjon

import kotlin.random.Random

class DungeonBuilder(
  nRows: Int,
  nCols: Int,
  dungeonLayout: DungeonLayout?,
  roomMin: Int,
  roomMax: Int,
  roomLayout: RoomLayout,
  corridorLayout: CorridorLayout,
  private val removeDeadends: Int,
  addStairs: Int,
  private val seed: Long,
) {
  private val nI: Int = nRows / 2
  private val nJ: Int = nCols / 2
  private val nRows = nI * 2
  private val nCols = nJ * 2

  private val random = Random(seed)

  val cellBuilder = CellBuilder(this.nRows, this.nCols, dungeonLayout)
  val roomBuilder = RoomBuilder(this.random, this.cellBuilder, nI, nJ, roomMin, roomMax, roomLayout)
  val corridorBuilder = CorridorBuilder(this.random, this.cellBuilder, nI, nJ, corridorLayout)
  val stairBuilder = StairBuilder(this.random, this.cellBuilder, this.corridorBuilder, nI, nJ, addStairs)

  fun generate() {
    cellBuilder.initializeCells()
    roomBuilder.placeRooms()
    roomBuilder.openRooms()
    roomBuilder.labelRooms()
    corridorBuilder.corridors()
    stairBuilder.placeStairs()
    cleanDungeon()
  }

  fun buildDungeon(): Dungeon =
    Dungeon(
      seed=seed, random=random,
      nRows=nRows, nCols=nCols,
      cells=cellBuilder.buildCells(),
      stairs=stairBuilder.buildStairs(),
      rooms=roomBuilder.buildRooms(),
      doors=roomBuilder.doorBuilder.buildDoors(),
    )

  private fun cleanDungeon() {
    if (removeDeadends > 0) {
      corridorBuilder.collapseTunnels(removeDeadends)
    }
    roomBuilder.doorBuilder.fixDoors()
    cellBuilder.emptyBlocks()
  }

}

data class Dungeon(
  val seed: Long,
  val random: Random,
  val nRows: Int,
  val nCols: Int,
  val cells: List<List<Cell>>,
  val stairs: List<Stair>,
  val rooms: List<Room>,
  val doors: List<Door>,
)
