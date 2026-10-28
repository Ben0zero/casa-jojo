package cl.duoc.casajojo.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/** Verde de "campo correcto" (el mismo de los estados "Pagado"/"Entregado"). */
private val ColorValido = Color(0xFF2F855A)

/**
 * Campo de formulario reutilizable: etiqueta + caja de texto + error debajo.
 *
 * - icono → ícono a la izquierda que indica qué se escribe (correo, candado…).
 * - error != null → borde rojo (isError), ícono de error a la derecha y el
 *   mensaje debajo (supportingText), como pide la guía formulario-registro.
 * - validado = true → el formulario ya se revisó; si el campo no tiene error y
 *   tiene texto, se marca con un ✓ verde a la derecha.
 * - esContrasena = true → oculta los caracteres y agrega el ojo Mostrar/Ocultar.
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
    icono: ImageVector? = null,
    validado: Boolean = false,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    accionIme: ImeAction = ImeAction.Next,
    esContrasena: Boolean = false,
    enabled: Boolean = true,
    onListo: (() -> Unit)? = null,
) {
    // rememberSaveable: "Mostrar contraseña" se mantiene aunque se gire el teléfono.
    var visible by rememberSaveable { mutableStateOf(false) }
    val correcto = validado && error == null && valor.isNotBlank()

    // Ícono de estado del campo: ⚠ si hay error, ✓ si ya se validó y está bien.
    val iconoEstado: (@Composable () -> Unit)? = when {
        error != null -> {
            { Icon(Icons.Filled.Error, contentDescription = "Campo con error", tint = MaterialTheme.colorScheme.error) }
        }
        correcto -> {
            { Icon(Icons.Filled.CheckCircle, contentDescription = "Campo correcto", tint = ColorValido) }
        }
        else -> null
    }

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
        leadingIcon = if (icono != null) {
            { Icon(icono, contentDescription = null) }
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
        // En contraseñas se muestran juntos el ícono de estado y el ojo.
        trailingIcon = if (esContrasena) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    iconoEstado?.invoke()
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                        )
                    }
                }
            }
        } else {
            iconoEstado
        },
        shape = RoundedCornerShape(12.dp),
        // ANIMACIÓN: cuando aparece o desaparece el mensaje de error, el campo
        // crece/decrece con una transición suave en vez de saltar.
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    )
}