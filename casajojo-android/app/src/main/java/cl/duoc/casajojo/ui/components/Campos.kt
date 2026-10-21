package cl.duoc.casajojo.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Campo de formulario reutilizable: etiqueta + caja de texto + error debajo.
 *
 * - error != null → borde rojo (isError) y el mensaje se muestra debajo
 *   (supportingText), como pide la guía formulario-registro.
 * - esContrasena = true → oculta los caracteres y agrega el botón Mostrar/Ocultar.
 * - tipoTeclado → abre el teclado adecuado (correo con @, numérico para el celular).
 * - accionIme → botón de la esquina del teclado: Next pasa al siguiente campo,
 *   Done cierra el teclado y ejecuta onListo (por ejemplo, enviar el formulario).
 */
@Composable
fun CampoTexto(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    error: String?,
    placeholder: String? = null,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    accionIme: ImeAction = ImeAction.Next,
    esContrasena: Boolean = false,
    enabled: Boolean = true,
    onListo: (() -> Unit)? = null,
) {
    // rememberSaveable: "Mostrar contraseña" se mantiene aunque se gire el teléfono.
    var visible by rememberSaveable { mutableStateOf(false) }

    Text(
        etiqueta,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        placeholder = if (placeholder != null) {
            { Text(placeholder) }
        } else {
            null
        },
        supportingText = if (error != null) {
            { Text(error, color = MaterialTheme.colorScheme.error) }
        } else {
            null
        },
        visualTransformation = if (esContrasena && !visible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (esContrasena) KeyboardType.Password else tipoTeclado,
            imeAction = accionIme,
        ),
        keyboardActions = KeyboardActions(onDone = { onListo?.invoke() }),
        trailingIcon = if (esContrasena) {
            {
                TextButton(onClick = { visible = !visible }) {
                    Text(if (visible) "Ocultar" else "Mostrar")
                }
            }
        } else {
            null
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}