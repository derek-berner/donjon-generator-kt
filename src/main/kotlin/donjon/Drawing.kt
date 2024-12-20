package net.bernerbits.dnd.donjon

import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.embed.swing.SwingFXUtils
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.scene.paint.ImagePattern
import javafx.scene.shape.ArcType
import javafx.scene.text.Text
import java.io.File
import java.util.concurrent.SynchronousQueue
import javax.imageio.ImageIO
import kotlin.random.Random

fun imageDungeon(dungeon: Dungeon, style: DungeonStyle): String {
  JFXPanel()
  val outputFilenameQueue = SynchronousQueue<String>()

  Platform.runLater {
    try {
      val image = scaleDungeon(dungeon, style)
      val canvas = Canvas(image.width.toDouble(), image.height.toDouble())
      val gc = canvas.graphicsContext2D
      val palette = style.mapStyle.palette

      // Create and draw the base layer
      val baseLayer = createBaseLayer(gc, dungeon, style, image, palette)
      // Fill the base layer and other features
      fillImage(gc, dungeon, style, image, palette)
      openCells(gc, dungeon, baseLayer, image, palette)
      drawWalls(gc, dungeon, image, palette)
      drawDoors(gc, dungeon, image, palette)
      if (dungeon.stairs.isNotEmpty())
        drawStairs(gc, dungeon, image, palette)
      drawLabels(gc, dungeon, image, palette)

      // Save the Canvas to a file
      val outputFilename = "${dungeon.seed}.png"
      saveCanvasToFile(canvas, outputFilename)
      outputFilenameQueue.put(outputFilename)
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      Platform.exit()
    }
  }
  return outputFilenameQueue.take()
}

fun saveCanvasToFile(canvas: Canvas, filename: String) {
  val writableImage = WritableImage(canvas.width.toInt(), canvas.height.toInt())
  canvas.snapshot(null, writableImage)
  val bufferImage = SwingFXUtils.fromFXImage(writableImage, null)
  ImageIO.write(bufferImage, "png", File(filename))
}

fun scaleDungeon(dungeon: Dungeon, style: DungeonStyle): DungeonImage {
  val cellSize = style.cellSize
  val width = ((dungeon.nCols + 1) * cellSize) + 1
  val height = ((dungeon.nRows + 1) * cellSize) + 1
  return DungeonImage(cellSize, style.mapStyle, width, height, width - 1, height - 1)
}

fun createBaseLayer(gc: GraphicsContext, dungeon: Dungeon, style: DungeonStyle, image: DungeonImage, palette: Palette): Image {
  val canvas = Canvas(image.width.toDouble(), image.height.toDouble()) // Create a new Canvas for base layer
  val baseGc = canvas.graphicsContext2D

  val max_x = image.maxX.toDouble()
  val max_y = image.maxY.toDouble()
  val dim = image.cellSize.toDouble()

  // Fill the base layer
  if (palette.openPattern != null) {
    baseGc.fill = palette.openPattern
    baseGc.fillRect(0.0, 0.0, max_x, max_y)
  } else if (palette.openTile != null) {
    for (r in 0 until dungeon.nRows) {
      for (c in 0 until dungeon.nCols) {
        baseGc.fill = selectTile(dungeon.random, palette.openTile, dim)
        baseGc.fillRect(c * dim, r * dim, (c + 1) * dim, (r + 1) * dim)
      }
    }
  } else if (palette.open != null) {
    baseGc.fill = palette.open
    baseGc.fillRect(0.0, 0.0, max_x, max_y)
  } else if (palette.background != null) {
    baseGc.fill = palette.background
    baseGc.fillRect(0.0, 0.0, max_x, max_y)
  } else {
    baseGc.fill = Color.WHITE
    baseGc.fillRect(0.0, 0.0, max_x, max_y)
  }

  // Draw open grid or grid, if specified
  val gridColor = palette.openGrid ?: palette.grid
  gridColor?.let {
    baseGc.stroke = it
    baseGc.lineWidth = 0.5
    drawGrid(baseGc, style, image, it)
  }

  val base = canvas.toWritableImage()
  gc.fill = ImagePattern(base, 0.0, 0.0, canvas.width, canvas.height, false)
  gc.fillRect(0.0, 0.0, max_x, max_y)

  if (palette.background != null) {
    gc.fill = palette.background
    baseGc.fillRect(0.0, 0.0, max_x, max_y)
  } else {
    gc.fill = Color.WHITE
    baseGc.fillRect(0.0, 0.0, max_x, max_y)
  }
  return base // Return the Canvas as the base layer
}

fun selectTile(random: Random, tile: ImagePattern, dim: Double): ImagePattern {
  val srcX = random.nextInt((tile.width / dim).toInt()) * dim
  val srcY = random.nextInt((tile.height / dim).toInt()) * dim
  return ImagePattern(tile.image, srcX, srcY, dim, dim, false)
}

fun drawGrid(gc: GraphicsContext, style: DungeonStyle, image: DungeonImage, color: Color) {
  when (style.grid) {
    Grid.SQUARE -> drawSquareGrid(gc, image, color)
    Grid.HEX -> drawHexGrid(gc, image, color)
    else -> {
      // do nothing
    }
  }
}

fun drawSquareGrid(gc: GraphicsContext, image: DungeonImage, color: Color) {
  val dim = image.cellSize
  gc.stroke = color
  gc.lineWidth = 0.1
  for (x in 0..image.maxX step dim) {
    gc.strokeLine(x.toDouble(), 0.0, x.toDouble(), image.maxY.toDouble())
  }
  for (y in 0..image.maxY step dim) {
    gc.strokeLine(0.0, y.toDouble(), image.maxX.toDouble(), y.toDouble())
  }
}

fun drawHexGrid(gc: GraphicsContext, image: DungeonImage, color: Color) {
  val dim = image.cellSize.toDouble()
  val dy = dim / 2.0
  val dx = dim / 3.4641016151 // This is approximately sqrt(3)
  val nCol = (image.width / (3 * dx)).toInt()
  val nRow = (image.height / dy).toInt()

  gc.stroke = color
  gc.lineWidth = 0.5

  for (i in 0 until nCol) {
    val x1 = i * (3 * dx)
    val x2 = x1 + dx
    val x3 = x1 + (3 * dx)

    for (j in 0 until nRow) {
      val y1 = j * dy
      val y2 = y1 + dy

      if ((i + j) % 2 == 0) {
        gc.strokeLine(x2, y1, x1, y2)
      } else {
        gc.strokeLine(x1, y1, x2, y2)
        gc.strokeLine(x2, y2, x3, y2)
      }
    }
  }
}

fun fillImage(gc: GraphicsContext, dungeon: Dungeon, style: DungeonStyle, image: DungeonImage, palette: Palette) {
  val maxX = image.maxX.toDouble()
  val maxY = image.maxY.toDouble()
  val cellSize = image.cellSize

  if (palette.fillPattern != null) {
    // Use fill pattern for the entire background
    gc.fill = palette.fillPattern
    gc.fillRect(0.0, 0.0, maxX, maxY)
  } else if (palette.fillTile != null) {
    // Fill with tile pattern, per dungeon cell
    for (r in 0..dungeon.nRows) {
      for (c in 0..dungeon.nCols) {
        val x = c * cellSize.toDouble()
        val y = r * cellSize.toDouble()
        gc.fill = palette.fillTile
        gc.fillRect(x, y, cellSize.toDouble(), cellSize.toDouble())
      }
    }
  } else if (palette.fill != null) {
    // Fill with a solid color if no pattern is set
    gc.fill = palette.fill
    gc.fillRect(0.0, 0.0, maxX, maxY)
  } else if (palette.background != null) {
    // Use background pattern if no fill pattern is defined
    gc.fill = palette.background
    gc.fillRect(0.0, 0.0, maxX, maxY)
  } else {
    // Default fill if none are defined
    gc.fill = Color.BLACK
    gc.fillRect(0.0, 0.0, maxX, maxY)
  }

  (palette.fillGrid ?: palette.grid)?.let {
    gc.stroke = it
    gc.lineWidth = 0.5
    drawGrid(gc, style, image, it)
  }
}

fun openCells(gc: GraphicsContext, dungeon: Dungeon, base: Image, image: DungeonImage, palette: Palette) {
  val basePattern = palette.openPattern ?: palette.open
  val dim = image.cellSize.toDouble()

  for (r in 0 until dungeon.nRows) {
    val y = r * dim

    for (c in 0 until dungeon.nCols) {
      val x = c * dim

      if (dungeon.cells[r][c].attributes.containsAny(OPENSPACE)) {
        // Use pattern if available, otherwise use the open color
        if (palette.openPattern != null) {
          gc.fill = basePattern
        } else {
          gc.fill = palette.open
        }

        gc.drawImage(
          base,
          x, y, dim + 1, dim + 1,
          x, y, dim + 1, dim + 1,
        )
      }
    }
  }
}

fun drawWalls(gc: GraphicsContext, dungeon: Dungeon, image: DungeonImage, palette: Palette) {
  val cellSize = image.cellSize
  val wallColor = palette.wall

  for (r in 0 until dungeon.nRows) {
    val y1 = r * cellSize
    val y2 = y1 + cellSize

    for (c in 0 until dungeon.nCols) {
      if (dungeon.cells[r][c].attributes.containsAny(OPENSPACE)) {
        val x1 = c * cellSize
        val x2 = x1 + cellSize

        // Draw walls based on adjacent cells
        if (!dungeon.cells[r - 1][c].attributes.containsAny(OPENSPACE)) {
          gc.stroke = wallColor
          gc.lineWidth = 0.5
          gc.strokeLine(x1.toDouble(), y1.toDouble(), x2.toDouble(), y1.toDouble())
        }

        if (!dungeon.cells[r][c - 1].attributes.containsAny(OPENSPACE)) {
          gc.stroke = wallColor
          gc.lineWidth = 0.5
          gc.strokeLine(x1.toDouble(), y1.toDouble(), x1.toDouble(), y2.toDouble())
        }

        if (!dungeon.cells[r][c + 1].attributes.containsAny(OPENSPACE)) {
          gc.stroke = wallColor
          gc.lineWidth = 0.5
          gc.strokeLine(x2.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
        }

        if (!dungeon.cells[r + 1][c].attributes.containsAny(OPENSPACE)) {
          gc.stroke = wallColor
          gc.lineWidth = 0.5
          gc.strokeLine(x1.toDouble(), y2.toDouble(), x2.toDouble(), y2.toDouble())
        }
      }
    }
  }
}

fun drawDoors(gc: GraphicsContext, dungeon: Dungeon, image: DungeonImage, palette: Palette) {
  val doors = dungeon.doors
  val cellSize = image.cellSize
  val archSize = cellSize / 6.0
  val doorThickness = cellSize / 4.0
  val trapThickness = cellSize / 3.0

  val archColor = palette.wall
  val doorColor = palette.door

  for (door in doors) {
    val (r, c) = door.row to door.col
    val x1 = c * cellSize.toDouble()
    val y1 = r * cellSize.toDouble()
    val x2 = x1 + cellSize.toDouble()
    val y2 = y1 + cellSize.toDouble()

    val xc = (x1 + x2) / 2
    val yc = (y1 + y2) / 2

    when {
      dungeon.cells[r][c - 1].attributes.containsAny(OPENSPACE) -> { // Vertical Door
        drawVerticalDoor(gc, door, archSize, doorThickness, trapThickness, y1, y2, xc, yc, archColor, doorColor)
      }
      else -> { // Horizontal Door
        drawHorizontalDoor(gc, door, archSize, doorThickness, trapThickness, x1, x2, xc, yc, archColor, doorColor)
      }
    }
  }
}

fun drawVerticalDoor(
  gc: GraphicsContext, door: Door, archSize: Double, doorThickness: Double, trapThickness: Double,
  y1: Double, y2: Double, xc: Double, yc: Double,
  archColor: Color, doorColor: Color
) {
  // Handle wall
  if (door.type.wall) {
    gc.stroke = archColor
    gc.lineWidth = 0.5
    gc.strokeLine(xc, y1, xc, y2)
  }

  // Handle secret
  if (door.type.secret) {
    gc.stroke = doorColor
    gc.lineWidth = 0.5
    gc.strokeLine(xc - 1, yc - doorThickness, xc + 2, yc - doorThickness)
    gc.strokeLine(xc - 2, yc - doorThickness + 1, xc - 2, yc - 1)
    gc.strokeLine(xc - 1, yc, xc + 1, yc)
    gc.strokeLine(xc + 2, yc + 1, xc + 2, yc + doorThickness - 1)
    gc.strokeLine(xc - 2, yc + doorThickness, xc + 1, yc + doorThickness)
  }

  // Handle arch
  if (door.type.arch) {
    gc.fill = archColor
    gc.fillRect(xc - 1, y1, 2.0, archSize)
    gc.fillRect(xc - 1, y2 - archSize, 2.0, archSize)
  }

  // Handle door
  if (door.type.door) {
    gc.stroke = doorColor
    gc.lineWidth = 0.5
    gc.strokeRect(xc - doorThickness, y1 + archSize + 1, doorThickness * 2, y2 - y1 - 2 * archSize - 2)
  }

  // Handle lock
  if (door.type.lock) {
    gc.stroke = doorColor
    gc.lineWidth = 0.5
    gc.strokeLine(xc, y1 + archSize + 1, xc, y2 - archSize - 1)
  }

  // Handle trap
  if (door.type.trap) {
    gc.stroke = doorColor
    gc.lineWidth = 0.5
    gc.strokeLine(xc - trapThickness, yc, xc + trapThickness, yc)
  }

  // Handle portcullis
  if (door.type.portc) {
    for (y in (y1 + archSize + 2).toInt() until (y2 - archSize).toInt() step 2) {
      gc.fillArc(xc, y.toDouble(), 1.0, 1.0, 0.0, 360.0, ArcType.ROUND)
    }
  }
}

fun drawHorizontalDoor(
  gc: GraphicsContext, door: Door, archSize: Double, doorThickness: Double, trapThickness: Double,
  x1: Double, x2: Double, xc: Double, yc: Double,
  archColor: Color, doorColor: Color
) {
  // Handle wall
  if (door.type.wall) {
    gc.stroke = archColor
    gc.lineWidth = 0.5
    gc.strokeLine(x1, yc, x2, yc)
  }

  // Handle secret
  if (door.type.secret) {
    gc.stroke = doorColor
    gc.lineWidth = 0.5
    gc.strokeLine(xc - doorThickness, yc - 2, xc - doorThickness, yc + 1)
    gc.strokeLine(xc - doorThickness + 1, yc + 2, xc - 1, yc + 2)
    gc.strokeLine(xc, yc - 1, xc, yc + 1)
    gc.strokeLine(xc + 1, yc - 2, xc + doorThickness - 1, yc - 2)
    gc.strokeLine(xc + doorThickness, yc - 1, xc + doorThickness, yc + 2)
  }

  // Handle arch
  if (door.type.arch) {
    gc.fill = archColor
    gc.fillRect(x1, yc - 1, archSize, 2.0)
    gc.fillRect(x2 - archSize, yc - 1, archSize, 2.0)
  }

  // Handle door
  if (door.type.door) {
    gc.stroke = doorColor
    gc.lineWidth = 0.5
    gc.strokeRect(xc + archSize + 1, yc - doorThickness, x2 - x1 - 2 * archSize - 2, doorThickness * 2)
  }

  // Handle lock
  if (door.type.lock) {
    gc.stroke = doorColor
    gc.lineWidth = 0.5
    gc.strokeLine(x1 + archSize + 1, yc, x2 - archSize - 1, yc)
  }

  // Handle trap
  if (door.type.trap) {
    gc.stroke = doorColor
    gc.lineWidth = 0.5
    gc.strokeLine(xc, yc - trapThickness, xc, yc + trapThickness)
  }

  // Handle portcullis
  if (door.type.portc) {
    for (x in (x1 + archSize + 2).toInt() until (x2 - archSize).toInt() step 2) {
      gc.fillArc(x.toDouble(), yc, 1.0, 1.0, 0.0, 360.0, ArcType.ROUND)
    }
  }
}

fun drawLabels(gc: GraphicsContext, dungeon: Dungeon, image: DungeonImage, palette: Palette) {
  val cellSize = image.cellSize
  gc.fill = palette.label

  for (r in 0..dungeon.nRows) {
    for (c in 0..dungeon.nCols) {
      if (dungeon.cells[r][c].attributes.containsAny(OPENSPACE)) {
        val label = dungeon.cells[r][c].label
        if (label != null) {
          // Calculate the position for the label
          val text = Text(label)
          text.font = gc.font
          val bounds = text.layoutBounds
          val x = (c * cellSize) + (cellSize / 2.0) - (bounds.width / 2.0)
          val y = (r * cellSize) + (cellSize / 2.0) + (bounds.height / 2.0) - 2
          // Draw the label on the canvas
          gc.fillText(label, x, y)
        }
      }
    }
  }
}

fun drawStairs(gc: GraphicsContext, dungeon: Dungeon, image: DungeonImage, palette: Palette) {
  val stairs = dungeon.stairs
  if (stairs.isEmpty()) return

  val dim = image.cellSize
  val sPx = dim / 2
  val tPx = (dim / 20) + 2

  gc.stroke = palette.stair
  gc.lineWidth = 1.0

  for (stair in stairs) {
    when {
      stair.nextRow > stair.row -> {
        // Stair going upward in rows
        val xc = ((stair.col + 0.5) * dim).toInt()
        val y1 = stair.row * dim
        val y2 = (stair.nextRow + 1) * dim

        for (y in y1 until y2 step tPx * 2) {
          val dx = if (stair.key == StairKey.DOWN) ((y - y1).toDouble() / (y2 - y1) * sPx).toInt() else sPx
          gc.strokeLine((xc - dx).toDouble(), y.toDouble(), (xc + dx).toDouble(), y.toDouble())
        }
      }
      stair.nextRow < stair.row -> {
        // Stair going downward in rows
        val xc = ((stair.col + 0.5) * dim).toInt()
        val y1 = (stair.row + 1) * dim
        val y2 = stair.nextRow * dim

        for (y in y1 downTo y2 step tPx * 2) {
          val dx = if (stair.key == StairKey.DOWN) ((y - y1).toDouble() / (y2 - y1) * sPx).toInt() else sPx
          gc.strokeLine((xc - dx).toDouble(), y.toDouble(), (xc + dx).toDouble(), y.toDouble())
        }
      }
      stair.nextCol > stair.col -> {
        // Stair going right in columns
        val x1 = stair.col * dim
        val x2 = (stair.nextCol + 1) * dim
        val yc = ((stair.row + 0.5) * dim).toInt()

        for (x in x1 until x2 step tPx * 2) {
          val dy = if (stair.key == StairKey.DOWN) ((x - x1).toDouble() / (x2 - x1) * sPx).toInt() else sPx
          gc.strokeLine(x.toDouble(), (yc - dy).toDouble(), x.toDouble(), (yc + dy).toDouble())
        }
      }
      stair.nextCol < stair.col -> {
        // Stair going left in columns
        val x1 = (stair.col + 1) * dim
        val x2 = stair.nextCol * dim
        val yc = ((stair.row + 0.5) * dim).toInt()

        for (x in x1 downTo x2 step tPx * 2) {
          val dy = if (stair.key == StairKey.DOWN) ((x - x1).toDouble() / (x2 - x1) * sPx).toInt() else sPx
          gc.strokeLine(x.toDouble(), (yc - dy).toDouble(), x.toDouble(), (yc + dy).toDouble())
        }
      }
    }
  }
}

fun Canvas.toWritableImage(): WritableImage {
  val writableImage = WritableImage(this.width.toInt(), this.height.toInt())
  this.snapshot(null, writableImage)
  return writableImage
}
