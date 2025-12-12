/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.appfunctions

import android.os.Build
import androidx.appsearch.app.EmbeddingVector
import androidx.appsearch.app.Features
import androidx.appsearch.app.GenericDocument
import java.util.Objects


object GenericDocumentToPlatformConverter {
    /** Translates a jetpack [androidx.appsearch.app.GenericDocument] into a platform [ ]. */
    fun toPlatformGenericDocument(
        jetpackDocument: GenericDocument
    ): android.app.appsearch.GenericDocument {
        Objects.requireNonNull(jetpackDocument)
        val platformBuilder =
            android.app.appsearch.GenericDocument.Builder<
                    android.app.appsearch.GenericDocument.Builder<*>
                    >(
                jetpackDocument.namespace,
                jetpackDocument.id,
                jetpackDocument.schemaType,
            )
        platformBuilder
            .setScore(jetpackDocument.score)
            .setTtlMillis(jetpackDocument.ttlMillis)
            .setCreationTimestampMillis(jetpackDocument.creationTimestampMillis)
        for (propertyName in jetpackDocument.propertyNames) {
            val property = jetpackDocument.getProperty(propertyName!!)
            if (property is Array<*> && property.isArrayOf<String>()) {
                platformBuilder.setPropertyString(propertyName, *property as Array<String?>)
            } else if (property is LongArray) {
                platformBuilder.setPropertyLong(propertyName, *property)
            } else if (property is DoubleArray) {
                platformBuilder.setPropertyDouble(propertyName, *property)
            } else if (property is BooleanArray) {
                platformBuilder.setPropertyBoolean(propertyName, *property)
            } else if (property is Array<*> && property.isArrayOf<ByteArray>()) {
                val byteValues = property
                // This is a patch for b/204677124, framework-appsearch in Android S and S_V2 will
                // crash if the user put a document with empty byte[][] or document[].
                if (
                    (Build.VERSION.SDK_INT == Build.VERSION_CODES.S ||
                            Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2) && byteValues.size == 0
                ) {
                    continue
                }
                platformBuilder.setPropertyBytes(propertyName, *byteValues as Array<out ByteArray>)
            } else if (property is Array<*> && property.isArrayOf<GenericDocument>()) {
                val documentValues = property
                // This is a patch for b/204677124, framework-appsearch in Android S and S_V2 will
                // crash if the user put a document with empty byte[][] or document[].
                if (
                    (Build.VERSION.SDK_INT == Build.VERSION_CODES.S ||
                            Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2) && documentValues.size == 0
                ) {
                    continue
                }
                val platformSubDocuments =
                    arrayOfNulls<android.app.appsearch.GenericDocument>(documentValues.size)
                for (j in documentValues.indices) {
                    platformSubDocuments[j] =
                        GenericDocumentToPlatformConverter.toPlatformGenericDocument(
                            documentValues[j] as GenericDocument
                        )
                }
                platformBuilder.setPropertyDocument(propertyName, *platformSubDocuments)
            } else if (property is Array<*> && property.isArrayOf<EmbeddingVector>()) {
                // TODO(b/326656531): Remove this once embedding search APIs are available.
                throw UnsupportedOperationException(
                    Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG +
                            " is not available on this AppSearch implementation."
                )
            } else {
                throw IllegalStateException(
                    String.format(
                        "Property \"%s\" has unsupported value type %s",
                        propertyName,
                        property!!.javaClass.toString(),
                    )
                )
            }
        }
        return platformBuilder.build()
    }
}
