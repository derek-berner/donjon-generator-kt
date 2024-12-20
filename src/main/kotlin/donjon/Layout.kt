package net.bernerbits.dnd.donjon

enum class DungeonLayout(val pattern: List<List<Int>>) {
  BOX(listOf(listOf(1, 1, 1), listOf(1, 0, 1), listOf(1, 1, 1))),
  CROSS(listOf(listOf(0, 1, 0), listOf(1, 1, 1), listOf(0, 1, 0))),
  ROUND(emptyList());
}

enum class Grid {
  SQUARE,
  HEX
}

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

}
