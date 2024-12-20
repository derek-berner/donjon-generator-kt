package net.bernerbits.dnd.donjon

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.scene.paint.ImagePattern
import java.io.File
import javax.imageio.ImageIO

// Dungeon Image Data Class
data class DungeonImage(
  val cellSize: Int,
  val mapStyle: MapStyle,
  var width: Int,
  var height: Int,
  var maxX: Int,
  var maxY: Int
)

// Palette Data Class
class Palette(
  wall: Color? = null,
  door: Color? = null,
  label: Color? = null,
  val open: Color? = null,
  fill: Color? = null,
  val grid: Color? = null,
  val openGrid: Color? = null,
  val fillGrid: Color? = null,
  stair: Color? = null,
  val openPattern: ImagePattern? = null,
  val openTile: ImagePattern? = null,
  val fillPattern: ImagePattern? = null,
  val fillTile: ImagePattern? = null,
  val background: ImagePattern? = null
) {

  val door: Color = door ?: fill ?: Color.BLACK
  val label: Color = label ?: fill ?: Color.BLACK
  val stair: Color = stair ?: wall ?: fill ?: Color.BLACK
  val wall: Color = wall ?: fill ?: Color.BLACK
  val fill: Color = fill ?: Color.BLACK

  companion object {
    fun defaultStandard(): Palette {
      // Example pattern initialization, replace with actual image paths
      // val defaultImagePattern = ImagePattern(Image("path/to/default/tile.png"))

      return Palette(
        fill = Color.web("#000000"),     // Black
        open = Color.web("#FFFFFF"),     // White
        openGrid = Color.web("#CCCCCC"), // Light Gray
        wall = Color.web("#2F4F4F"),     // Dark Slate Gray
        door = Color.web("#8B4513"),     // Saddle Brown
        label = Color.web("#0000FF"),    // Blue
        grid = Color.web("#808080"),     // Gray
        stair = Color.web("#FFD700"),    // Gold
        fillPattern = null,
        fillTile = null,
        openPattern = null,
        background = null // Ideally, use another different pattern for testing
      )
    }
  }
}

fun saveImageToFile(image: WritableImage, filename: String) {
  val bufferedImage = SwingFXUtils.fromFXImage(image, null)
  val file = File(filename)
  val format = filename.substringAfterLast('.', "png")
  ImageIO.write(bufferedImage, format, file)
  println("Image saved as $filename successfully.")
}
