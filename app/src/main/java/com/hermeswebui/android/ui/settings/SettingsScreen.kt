package com.hermeswebui.android.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hermeswebui.android.data.ServerProfile

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    initialServerUrl: String,
    isConfigured: Boolean,
    backgroundReconnectEnabled: Boolean,
    serverProfiles: List<ServerProfile>,
    onSave: (String) -> Unit,
    onResetSession: () -> Unit,
    onDismiss: () -> Unit,
    onSetBackgroundReconnect: (Boolean) -> Unit,
    onAddProfile: (String, String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onRenameProfile: (String, String) -> Unit,
    onEditProfile: (String, String, String) -> Unit,
    onSwitchProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    var showAddProfileDialog by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<ServerProfile?>(null) }
    var profileToEdit by remember { mutableStateOf<ServerProfile?>(null) }
    var showResetSessionConfirm by remember { mutableStateOf(false) }
    var serverUrl by remember(initialServerUrl, isConfigured) {
        mutableStateOf(if (isConfigured) initialServerUrl else "")
    }

    // --- Dialogs ---
    if (showAddProfileDialog) {
        AddServerProfileDialog(
            existingProfiles = serverProfiles,
            onConfirm = { name, url -> onAddProfile(name, url); showAddProfileDialog = false },
            onDismiss = { showAddProfileDialog = false }
        )
    }
    profileToEdit?.let { editing ->
        EditProfileDialog(
            currentName = editing.name,
            currentUrl = editing.url,
            onConfirm = { newName, newUrl ->
                onEditProfile(editing.id, newName, newUrl)
                profileToEdit = null
            },
            onDismiss = { profileToEdit = null }
        )
    }
    profileToDelete?.let { deleting ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text("Delete server?") },
            text = { Text("Remove \"${deleting.name}\" from your saved servers?") },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteProfile(deleting.id)
                    profileToDelete = null
                }) { Text("Delete") }
            }
        )
    }
    if (showResetSessionConfirm) {
        AlertDialog(
            onDismissRequest = { showResetSessionConfirm = false },
            title = { Text("Reset web session?") },
            text = {
                Text("This will clear all cookies, local storage, and cached data. You will be signed out of Hermes.")
            },
            dismissButton = {
                TextButton(onClick = { showResetSessionConfirm = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = { onResetSession(); showResetSessionConfirm = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Reset") }
            }
        )
    }

    // --- Full-screen settings page ---
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close settings"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
            ) {
                if (!isConfigured) {
                    // First-run: connect to server
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Connect to Hermes",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Enter your Hermes server URL to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            singleLine = true,
                            label = { Text("Hermes server URL") },
                            placeholder = { Text("https://hermes.example.com") },
                            supportingText = { Text("HTTP or HTTPS. Host is automatically allowlisted.") }
                        )
                        Button(
                            onClick = { onSave(serverUrl.trim()) },
                            enabled = serverUrl.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Connect")
                        }
                    }
                } else {
                    // ── Servers ──────────────────────────────────────────
                    SettingsSectionHeader("Servers")

                    val activeProfile = serverProfiles.firstOrNull { profile ->
                        profile.url.trimEnd('/').equals(initialServerUrl.trimEnd('/'), ignoreCase = true)
                    } ?: serverProfiles.firstOrNull { it.isActive }
                    val sortedProfiles = listOfNotNull(activeProfile) +
                        serverProfiles.filter { it.id != activeProfile?.id }

                    if (sortedProfiles.isEmpty()) {
                        ListItem(
                            headlineContent = { Text("Current server", maxLines = 1) },
                            supportingContent = {
                                Text(
                                    initialServerUrl,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            trailingContent = { ServerCurrentBadge() },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            sortedProfiles.forEach { profile ->
                                val isCurrent = profile.id == activeProfile?.id
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            profile.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            profile.url,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    trailingContent = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isCurrent) ServerCurrentBadge()
                                            IconButton(onClick = { profileToDelete = profile }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete \"${profile.name}\""
                                                )
                                            }
                                        }
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = if (isCurrent)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = { if (!isCurrent) onSwitchProfile(profile.id) },
                                            onLongClick = { profileToEdit = profile }
                                        )
                                        .alpha(if (isCurrent) 1f else 0.9f)
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Tap to switch server. Long-press to edit.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    ListItem(
                        headlineContent = { Text("Add server") },
                        leadingContent = {
                            Icon(Icons.Default.Add, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showAddProfileDialog = true }
                    )

                    HorizontalDivider()

                    // ── Application ───────────────────────────────────────
                    SettingsSectionHeader("Application")

                    ListItem(
                        headlineContent = { Text("Background reconnect notification") },
                        supportingContent = {
                            Text("Show a notification while reconnecting after an app switch")
                        },
                        trailingContent = {
                            Switch(
                                checked = backgroundReconnectEnabled,
                                onCheckedChange = onSetBackgroundReconnect
                            )
                        },
                        modifier = Modifier.clickable {
                            onSetBackgroundReconnect(!backgroundReconnectEnabled)
                        }
                    )

                    HorizontalDivider()

                    // ── Session ───────────────────────────────────────────
                    SettingsSectionHeader("Session")

                    ListItem(
                        headlineContent = {
                            Text(
                                "Reset web session",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        supportingContent = {
                            Text("Clear cookies, local storage, and cached data. You will be signed out.")
                        },
                        modifier = Modifier.clickable { showResetSessionConfirm = true }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
internal fun ServerCurrentBadge() {
    SuggestionChip(
        onClick = {},
        label = { Text("Current", style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
internal fun EditProfileDialog(
    currentName: String,
    currentUrl: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    var url by remember(currentUrl) { mutableStateOf(currentUrl) }
    val isValidUrl = url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Server name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Server URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text(
                            if (!isValidUrl && url.isNotBlank()) "Must start with http:// or https://"
                            else "HTTP or HTTPS"
                        )
                    },
                    isError = !isValidUrl && url.isNotBlank()
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && isValidUrl) onConfirm(name, url) },
                enabled = name.isNotBlank() && isValidUrl
            ) { Text("Save") }
        }
    )
}

@Composable
internal fun AddServerProfileDialog(
    existingProfiles: List<ServerProfile>,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var profileName by remember { mutableStateOf("") }
    var profileUrl by remember { mutableStateOf("") }
    val trimmedName = profileName.trim()
    val trimmedUrl = profileUrl.trim()
    val normalizedUrl = trimmedUrl.trimEnd('/').lowercase()
    val isValidUrl = trimmedUrl.isNotBlank() &&
        (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://"))
    val isDuplicateUrl = existingProfiles.any {
        it.url.trim().trimEnd('/').lowercase() == normalizedUrl
    }
    val isDuplicateName = trimmedName.isNotBlank() &&
        existingProfiles.any { it.name.trim().equals(trimmedName, ignoreCase = true) }
    val canSubmit = isValidUrl && !isDuplicateUrl && !isDuplicateName

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Server name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text(
                            if (isDuplicateName) "A server with this name already exists"
                            else "Optional friendly name"
                        )
                    },
                    isError = isDuplicateName
                )
                OutlinedTextField(
                    value = profileUrl,
                    onValueChange = { profileUrl = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("https://hermes.example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text(
                            if (!isValidUrl && trimmedUrl.isNotBlank()) "Must start with http:// or https://"
                            else if (isDuplicateUrl) "A server with this URL already exists"
                            else "HTTP or HTTPS"
                        )
                    },
                    isError = (!isValidUrl && trimmedUrl.isNotBlank()) || isDuplicateUrl
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSubmit) {
                        onConfirm(trimmedName.ifBlank { trimmedUrl }, trimmedUrl)
                        onDismiss()
                    }
                },
                enabled = canSubmit
            ) { Text("Add") }
        }
    )
}

