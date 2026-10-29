package com.example.eyirfitnessapp.ui.screens.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.ui.components.PrimaryButton
import com.example.eyirfitnessapp.ui.components.ScreenHeader
import com.example.eyirfitnessapp.ui.components.SecondaryButton
import com.example.eyirfitnessapp.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Spec §23. There is no backend in this build, so this screen does exactly one
 * honest thing: stores the email in the local profile for a future cloud sync.
 * No password is ever persisted (spec §30) and no sign-in is faked (spec §34).
 * Guest mode is simply the local diary itself.
 */
@Composable
fun LoginScreen(repository: AppRepository, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader(title = "Sign in", onBack = onBack)
        Spacer(Modifier.height(Spacing.xxl))

        Text(
            "Accounts and cloud sync aren't available yet. Saving your email keeps it on this " +
                "device only, so you're ready when sync launches.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.xl))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                error = null
            },
            label = { Text("Email") },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Spacing.xl))

        PrimaryButton(
            text = if (saving) "Saving…" else "Save email to this device",
            enabled = !saving,
            onClick = {
                val trimmed = email.trim()
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
                    error = "Enter a valid email address"
                } else {
                    saving = true
                    scope.launch {
                        val profile = repository.profileOnce()
                        if (profile == null) {
                            error = "No local profile found — finish onboarding first"
                            saving = false
                        } else {
                            repository.saveProfile(profile.copy(email = trimmed))
                            saving = false
                            onBack()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Spacing.md))
        SecondaryButton(text = "Continue as guest", onClick = onBack, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.xl))

        Text(
            "Passwords are never stored. Everything you log stays on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
