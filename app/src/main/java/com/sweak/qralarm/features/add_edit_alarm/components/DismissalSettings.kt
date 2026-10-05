package com.sweak.qralarm.features.add_edit_alarm.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sweak.qralarm.R
import com.sweak.qralarm.core.domain.alarm.DismissalMethod
import com.sweak.qralarm.core.domain.recognition.ObjectCategories

@Composable
fun DismissalSettings(
    method: DismissalMethod,
    categoryId: String?,
    showSelectionError: Boolean,
    onMethodSelected: (DismissalMethod) -> Unit,
    onObjectSelected: (String) -> Unit
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    fun selectMethod(option: DismissalMethod) {
        onMethodSelected(option)
        if (option == DismissalMethod.OBJECT) {
            query = ""
            showPicker = true
        }
    }
    Column(Modifier.padding(16.dp)) {
        Text(stringResource(R.string.dismissal_method), style = MaterialTheme.typography.titleMedium)
        DismissalMethod.entries.forEach { option ->
            Row(
                Modifier.fillMaxWidth().clickable { selectMethod(option) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = method == option, onClick = { selectMethod(option) })
                Text(stringResource(when (option) {
                    DismissalMethod.NONE -> R.string.dismissal_none
                    DismissalMethod.CODE -> R.string.use_qr_bar_code
                    DismissalMethod.OBJECT -> R.string.dismissal_object
                }))
            }
        }
        if (method == DismissalMethod.OBJECT) {
            TextButton(onClick = { query = ""; showPicker = true }) {
                Text(categoryId?.let(ObjectCategories::displayName)
                    ?: stringResource(R.string.choose_object))
            }
            Text(stringResource(R.string.object_recognition_description))
            if (showSelectionError) {
                Text(stringResource(R.string.choose_object_required), color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(stringResource(R.string.choose_object)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.search_objects)) },
                        singleLine = true
                    )
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(ObjectCategories.ids.filter { it.contains(query.trim(), ignoreCase = true) }, key = { it }) { id ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    onObjectSelected(id)
                                    showPicker = false
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = categoryId == id, onClick = {
                                    onObjectSelected(id)
                                    showPicker = false
                                })
                                Text(ObjectCategories.displayName(id))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
