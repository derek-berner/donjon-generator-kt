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

fun saveImageToFile(image: WritableImage, filename: String) {
  val bufferedImage = SwingFXUtils.fromFXImage(image, null)
  val file = File(filename)
  val format = filename.substringAfterLast('.', "png")
  ImageIO.write(bufferedImage, format, file)
  println("Image saved as $filename successfully.")
}
