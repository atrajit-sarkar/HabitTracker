package it.atraj.habittracker.social.ui.components

import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.syntax.Prism4jTheme
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j

/**
 * A composable that renders Markdown text with full support including:
 * - Math equations (LaTeX)
 * - Tables
 * - Strikethrough
 * - Task lists
 * - HTML
 * - Images
 * - Code syntax highlighting
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified
) {
    val context = LocalContext.current
    
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(ImagesPlugin.create())
            .usePlugin(HtmlPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(context))
            // Math and syntax highlighting temporarily disabled
            // .usePlugin(JLatexMathPlugin.create(style.fontSize.value) { })
            // .usePlugin(SyntaxHighlightPlugin.create(Prism4j(), Prism4jThemeDefault.create(0)))
            .build()
    }
    
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                textSize = style.fontSize.value
                if (color != Color.Unspecified) {
                    setTextColor(color.toArgb())
                }
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, markdown)
            if (color != Color.Unspecified) {
                textView.setTextColor(color.toArgb())
            }
        }
    )
}
