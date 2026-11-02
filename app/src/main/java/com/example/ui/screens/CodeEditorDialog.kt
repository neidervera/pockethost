package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EditorState

@Composable
fun CodeEditorDialog(
    editorState: EditorState,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onExecuteCode: () -> Unit = {},
    onExecute24x7: () -> Unit = {},
    onDismissResult: () -> Unit = {},
    onClose: () -> Unit
) {
    if (!editorState.isOpen || editorState.file == null) return

    val isModified = editorState.content != editorState.originalContent
    val lineCount by remember(editorState.content) {
        derivedStateOf {
            editorState.content.count { it == '\n' } + 1
        }
    }

    val isRunnable = editorState.file.extension.lowercase() in listOf("py", "cpp", "c", "cc")

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BackgroundDark
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Editor Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar editor",
                            tint = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = editorState.file.name,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (isModified) {
                                Text(
                                    text = " • modificado",
                                    color = CyanNeon,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Text(
                            text = "$lineCount líneas • ${editorState.content.length} caracteres",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Run Code Button (for Python and C++)
                    if (isRunnable) {
                        Button(
                            onClick = onExecuteCode,
                            enabled = !editorState.isRunningCode,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanNeon,
                                contentColor = BackgroundDark,
                                disabledContainerColor = SurfaceContainerDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_run_code")
                        ) {
                            if (editorState.isRunningCode) {
                                CircularProgressIndicator(
                                    color = BackgroundDark,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ejecutar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))

                        // 24/7 Continuous Execution Button
                        Button(
                            onClick = onExecute24x7,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldActive,
                                contentColor = BackgroundDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_run_247")
                        ) {
                            Text("24/7", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Save Button
                    Button(
                        onClick = onSave,
                        enabled = !editorState.isSaving && isModified,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldActive,
                            contentColor = BackgroundDark,
                            disabledContainerColor = SurfaceContainerDark,
                            disabledContentColor = TextSecondary.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_save_code")
                    ) {
                        if (editorState.isSaving) {
                            CircularProgressIndicator(
                                color = BackgroundDark,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Guardar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Quick Code Insertion Bar (for mobile typing!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceContainerDark)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickSnippets = if (isRunnable) {
                        listOf(
                            "print(", "def ", "import ", "from ", "class ", "return ",
                            "if ", "else:", "for ", "in ", "range(", "{", "}", "#include <",
                            "std::cout <<", "std::cin >>", "int main() {", "std::vector<",
                            "==", "!=", "=", ":", ";", "\"", "'", "(", ")", "[", "]"
                        )
                    } else {
                        listOf(
                            "<div>", "</div>", "<h1>", "<p>", "<script>", "</script>",
                            "<style>", "{", "}", "(", ")", "[", "]", "=", "\"",
                            "class=\"\"", "id=\"\"", "const ", "function()", "=>",
                            "<!-- -->", "/* */", ";", ":"
                        )
                    }

                    for (snippet in quickSnippets) {
                        Box(
                            modifier = Modifier
                                .background(SurfaceDark, RoundedCornerShape(6.dp))
                                .border(1.dp, BorderDark, RoundedCornerShape(6.dp))
                                .clickable {
                                    onContentChange(editorState.content + snippet)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = snippet,
                                color = CyanNeon,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Code Area with Line Numbers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF06090E))
                ) {
                    // Line numbers column
                    val lineNumbersText = (1..lineCount).joinToString("\n") { it.toString() }
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .background(Color(0xFF090D14))
                            .border(1.dp, Color(0xFF161E2E))
                            .padding(vertical = 12.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = lineNumbersText,
                            color = TextSecondary.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Main text editor
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        BasicTextField(
                            value = editorState.content,
                            onValueChange = onContentChange,
                            textStyle = TextStyle(
                                color = Color(0xFFE6EDF3),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            ),
                            cursorBrush = SolidColor(CyanNeon),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("code_editor_textarea")
                        )
                    }
                }

                // Live Terminal Output Drawer when execution completes
                editorState.executionResult?.let { result ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFF05070A))
                            .border(1.dp, CyanNeon.copy(alpha = 0.4f))
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "TERMINAL: ${result.language.uppercase()}",
                                        color = CyanNeon,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${result.durationMs}ms",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                    if (result.installedLibs.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "[LIBS] ${result.installedLibs.joinToString()}",
                                            color = EmeraldActive,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = onDismissResult,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar terminal", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Column {
                                    if (result.stdout.isNotEmpty()) {
                                        Text(
                                            text = result.stdout,
                                            color = Color(0xFF58A6FF),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (result.stderr.isNotEmpty()) {
                                        Text(
                                            text = result.stderr,
                                            color = CrimsonError,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
