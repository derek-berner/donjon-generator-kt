package net.bernerbits.dnd.donjon

import java.util.EnumSet
import kotlin.random.Random

class StairBuilder(
  val random: Random,
  val cellBuilder: CellBuilder,
  val corridorBuilder: CorridorBuilder,

  val nI: Int,
  val nJ: Int,
  val addStairs: Int
) {
  val stairs = mutableListOf<MutableStair>()

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
          cellBuilder.cells[r][c].attributes.add(CellAttribute.STAIR_DN)
          cellBuilder.cells[r][c].label = "d"
          stair.key = StairKey.DOWN
        } else {
          cellBuilder.cells[r][c].attributes.add(CellAttribute.STAIR_UP)
          cellBuilder.cells[r][c].label = "u"
          stair.key = StairKey.UP
        }
        this.stairs.add(stair)
      }
    }
  }

  private fun stairEnds(): MutableList<MutableStair> {
    val list = mutableListOf<MutableStair>()

    for (i in 0 until nI) {
      val r = (i * 2) + 1
      for (j in 0 until nJ) {
        val c = (j * 2) + 1

        if (cellBuilder.cells[r][c].attributes == EnumSet.of(CellAttribute.CORRIDOR)) {
          for (dir in Direction.entries) {
            if (corridorBuilder.checkTunnel(r, c, dir.stairEnd)) {
              val end = MutableStair(r, c).apply {
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

  fun buildStairs(): List<Stair> {
    return stairs.map { it.toStair() }
  }
}

enum class StairKey { UP, DOWN }

class MutableStair(var row: Int, var col: Int, var nextRow: Int = 0, var nextCol: Int = 0, var key: StairKey? = null) {
  fun toStair() = Stair(row=row, col=col, nextRow=nextRow, nextCol=nextCol, key=key!!)
}

data class Stair(val row: Int, val col: Int, val nextRow: Int, val nextCol: Int, val key: StairKey)

data class StairEnd(
  override val corridor: List<Pair<Int, Int>>,
  override val walled: List<Pair<Int, Int>>,
  override val close: List<Pair<Int, Int>>,
  val next: Pair<Int, Int>
): TunnelCheck
