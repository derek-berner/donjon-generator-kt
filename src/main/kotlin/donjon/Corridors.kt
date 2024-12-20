package net.bernerbits.dnd.donjon

import java.util.EnumSet
import kotlin.random.Random

class CorridorBuilder(
  val random: Random,
  val cellBuilder: CellBuilder,
  val nI: Int,
  val nJ: Int,
  val corridorLayout: CorridorLayout
) {

  fun checkTunnel(r: Int, c: Int, check: TunnelCheck): Boolean {
    return check.corridor.all { (dr, dc) -> cellBuilder.cells[r + dr][c + dc].attributes == EnumSet.of(CellAttribute.CORRIDOR) } &&
      check.walled.all { (dr, dc) -> !(cellBuilder.cells.getOrNull(r + dr)?.getOrNull(c + dc)?.attributes?.containsAny(OPENSPACE) ?: true) }
  }

  fun tunnel(i: Int, j: Int, lastDir: Direction? = null) {
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
    if (nextR < 0 || nextR > cellBuilder.nRows || nextC < 0 || nextC > cellBuilder.nCols) return false

    val rRange = minOf(midR, nextR)..maxOf(midR, nextR)
    val cRange = minOf(midC, nextC)..maxOf(midC, nextC)

    for (r in rRange) {
      for (c in cRange) {
        if (cellBuilder.cells[r][c].attributes.any { it in BLOCK_CORR }) return false
      }
    }
    return true
  }

  private fun delveTunnel(thisR: Int, thisC: Int, nextR: Int, nextC: Int): Boolean {
    val rRange = minOf(thisR, nextR)..maxOf(thisR, nextR)
    val cRange = minOf(thisC, nextC)..maxOf(thisC, nextC)

    for (r in rRange) {
      for (c in cRange) {
        cellBuilder.cells[r][c].attributes.remove(CellAttribute.ENTRANCE)
        cellBuilder.cells[r][c].attributes.add(CellAttribute.CORRIDOR)
      }
    }
    return true
  }

  fun collapseTunnels(p: Int) {
    if (p == 0) return
    val removeAll = p == 100

    for (i in 0 until nI) {
      val r = (i * 2) + 1
      for (j in 0 until nJ) {
        val c = (j * 2) + 1

        if (cellBuilder.cells[r][c].attributes.containsAny(OPENSPACE) && !cellBuilder.cells[r][c].attributes.containsAny(STAIRS)) {
          if (removeAll || random.nextInt(100) < p) {
            collapse(r, c)
          }
        }
      }
    }
  }

  fun corridors() {
    for (i in 1 until nI) {
      val r = (i * 2) + 1
      for (j in 1 until nJ) {
        val c = (j * 2) + 1

        if (!cellBuilder.cells[r][c].attributes.contains(CellAttribute.CORRIDOR)) {
          tunnel(i, j)
        }
      }
    }
  }

  fun collapse(r: Int, c: Int) {
    if (!cellBuilder.cells[r][c].attributes.containsAny(OPENSPACE)) return

    for (dir in Direction.entries) {
      val check = dir.closeEnd
      if (checkTunnel(r, c, check)) {
        check.close.forEach { (dr, dc) ->
          cellBuilder.cells[r + dr][c + dc] = MutableCell()
        }
        check.recurse.let { (dr, dc) -> collapse(r + dr, c + dc) }
      }
    }
  }

}

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

enum class CorridorLayout(val probability: Int) {
  LABYRINTH(0),
  BENT(50),
  STRAIGHT(100);
}
