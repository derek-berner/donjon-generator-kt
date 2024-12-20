package net.bernerbits.dnd.donjon

import java.util.EnumSet
import kotlin.math.sqrt

class CellBuilder(
  val nRows: Int,
  val nCols: Int,
  val dungeonLayout: DungeonLayout?
) {

  val cells = Array(nRows + 1) { Array(nCols + 1) { MutableCell() } }

  fun emptyBlocks() {
    for (r in 0..nRows) {
      for (c in 0..nCols) {
        if (cells[r][c].attributes.contains(CellAttribute.BLOCKED)) {
          cells[r][c] = MutableCell()
        }
      }
    }
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

  fun buildCells(): List<List<Cell>> {
    return cells.map{ row -> row.map { cell -> cell.toCell() } }
  }
}

class MutableCell(
  val attributes: EnumSet<CellAttribute> = EnumSet.noneOf(CellAttribute::class.java),
  var roomId: Int = 0,
  var label: String? = null
) {
  fun toCell() = Cell(attributes, roomId, label)
}

data class Cell(
  val attributes: EnumSet<CellAttribute>,
  val roomId: Int,
  val label: String?
)

enum class CellAttribute {
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

fun EnumSet<CellAttribute>.containsAny(other: EnumSet<CellAttribute>): Boolean {
  return this.intersect(other).isNotEmpty()
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
