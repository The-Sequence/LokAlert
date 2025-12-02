package com.mobprog.lokalert

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSection(
    onSearch: ((String) -> Unit)? = null,
    onSuggestionClick: ((String) -> Unit)? = null
) {
    var searchText by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var userHasSelectedSuggestion by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Places Client Setup
    val placesClient = remember {
        if (Places.isInitialized()) Places.createClient(context) else null
    }
    val token = remember { AutocompleteSessionToken.newInstance() }

    // Autocomplete Logic
    LaunchedEffect(searchText) {
        if (userHasSelectedSuggestion) {
            userHasSelectedSuggestion = false
            return@LaunchedEffect
        }
        if (searchText.isNotEmpty() && placesClient != null) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setCountries("PH") // Change country code if needed
                .setSessionToken(token)
                .setQuery(searchText)
                .build()

            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    suggestions = response.autocompletePredictions.map { it.getFullText(null).toString() }
                    expanded = suggestions.isNotEmpty()
                }
                .addOnFailureListener {
                    suggestions = emptyList()
                    expanded = false
                }
        } else {
            suggestions = emptyList()
            expanded = false
        }
    }

    // UI: A Floating Card Style
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp) // Outer padding from screen edges
            .padding(top = 48.dp) // Push down from status bar to avoid overlap
    ) {
        Surface(
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search locations...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.clickable {
                                    searchText = ""
                                    expanded = false
                                    keyboardController?.hide()
                                }
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSearch?.invoke(searchText)
                            expanded = false
                            keyboardController?.hide()
                        }
                    )
                )

                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        suggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        userHasSelectedSuggestion = true
                                        searchText = suggestion
                                        expanded = false
                                        onSuggestionClick?.invoke(suggestion)
                                        keyboardController?.hide()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location suggestion",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = suggestion,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}