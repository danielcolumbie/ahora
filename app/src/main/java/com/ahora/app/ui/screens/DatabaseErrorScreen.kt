package com.ahora.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ahora.app.ui.theme.Spacing

/**
 * La base de datos no pudo abrirse (migración fallida o la BD es de una
 * versión más nueva que esta app: Room lanza IllegalStateException).
 * Se muestra esto en vez de crashear; no se borró ninguna tarea
 * (auditoría 1.26.0). La salida honesta es actualizar la app.
 */
@Composable
fun DatabaseErrorScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No se pudo abrir tu lista",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(Spacing.m))
        Text(
            text = "Tus datos parecen ser de una versión más nueva de Ahora. " +
                "Instala la última versión de la app para recuperarlos.\n\n" +
                "No se borró ninguna tarea.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
