package io.github.uplush.dingoobox

import android.content.SharedPreferences
import android.util.AtomicFile
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

internal interface DingooPreferences {
    val all: Map<String, Any>

    fun contains(
        key: String
    ): Boolean

    fun getBoolean(
        key: String,
        defaultValue: Boolean
    ): Boolean

    fun getFloat(
        key: String,
        defaultValue: Float
    ): Float

    fun getInt(
        key: String,
        defaultValue: Int
    ): Int

    fun getLong(
        key: String,
        defaultValue: Long
    ): Long

    fun getString(
        key: String,
        defaultValue: String?
    ): String?

    fun getStringSet(
        key: String,
        defaultValue: Set<String>?
    ): Set<String>?

    fun edit(): Editor

    interface Editor {
        fun putBoolean(
            key: String,
            value: Boolean
        ): Editor

        fun putFloat(
            key: String,
            value: Float
        ): Editor

        fun putInt(
            key: String,
            value: Int
        ): Editor

        fun putLong(
            key: String,
            value: Long
        ): Editor

        fun putString(
            key: String,
            value: String?
        ): Editor

        fun putStringSet(
            key: String,
            value: Set<String>?
        ): Editor

        fun remove(
            key: String
        ): Editor

        fun clear(): Editor

        fun commit(): Boolean

        fun apply()
    }
}

internal class DingooFilePreferences private constructor(
    private val file: File,
    private val legacyPreferences:
        SharedPreferences?
) : DingooPreferences {
    private val lock =
        Any()

    private var values:
        MutableMap<String, Any> =
        loadInitialValues()

    override val all: Map<String, Any>
        get() =
            synchronized(lock) {
                values.mapValues {
                        (_, value) ->
                    copyValue(value)
                }
            }

    override fun contains(
        key: String
    ): Boolean {
        return synchronized(lock) {
            values.containsKey(key)
        }
    }

    override fun getBoolean(
        key: String,
        defaultValue: Boolean
    ): Boolean {
        return synchronized(lock) {
            values[key] as? Boolean
                ?: defaultValue
        }
    }

    override fun getFloat(
        key: String,
        defaultValue: Float
    ): Float {
        return synchronized(lock) {
            when (
                val value =
                    values[key]
            ) {
                is Float ->
                    value

                is Number ->
                    value.toFloat()

                else ->
                    defaultValue
            }
        }
    }

    override fun getInt(
        key: String,
        defaultValue: Int
    ): Int {
        return synchronized(lock) {
            when (
                val value =
                    values[key]
            ) {
                is Int ->
                    value

                is Number ->
                    value.toInt()

                else ->
                    defaultValue
            }
        }
    }

    override fun getLong(
        key: String,
        defaultValue: Long
    ): Long {
        return synchronized(lock) {
            when (
                val value =
                    values[key]
            ) {
                is Long ->
                    value

                is Number ->
                    value.toLong()

                else ->
                    defaultValue
            }
        }
    }

    override fun getString(
        key: String,
        defaultValue: String?
    ): String? {
        return synchronized(lock) {
            values[key] as? String
                ?: defaultValue
        }
    }

    override fun getStringSet(
        key: String,
        defaultValue: Set<String>?
    ): Set<String>? {
        return synchronized(lock) {
            val value =
                values[key]

            if (value is Set<*>) {
                value
                    .filterIsInstance<String>()
                    .toSet()
            } else {
                defaultValue?.toSet()
            }
        }
    }

    override fun edit():
        DingooPreferences.Editor {
        return EditorImpl()
    }

    private fun loadInitialValues():
        MutableMap<String, Any> {
        if (file.isFile) {
            val storedValues =
                readValues()

            if (storedValues != null) {
                return storedValues
            }
        }

        val importedValues =
            normalizeValues(
                legacyPreferences
                    ?.all
                    .orEmpty()
            )

        if (
            importedValues.isNotEmpty() &&
            !writeValues(importedValues)
        ) {
            Log.e(
                LOG_TAG,
                "Unable to import legacy preferences: ${
                    file.absolutePath
                }"
            )
        }

        return importedValues
    }

    private fun readValues():
        MutableMap<String, Any>? {
        return runCatching {
            val jsonText =
                AtomicFile(file)
                    .openRead()
                    .bufferedReader(
                        Charsets.UTF_8
                    )
                    .use { reader ->
                        reader.readText()
                    }

            val root =
                JSONObject(jsonText)

            val entries =
                root.optJSONObject(
                    VALUES_KEY
                ) ?: JSONObject()

            val result =
                linkedMapOf<String, Any>()

            val keys =
                entries.keys()

            while (keys.hasNext()) {
                val key =
                    keys.next()

                val encodedValue =
                    entries.optJSONObject(
                        key
                    ) ?: continue

                val decodedValue =
                    decodeValue(
                        encodedValue
                    ) ?: continue

                result[key] =
                    decodedValue
            }

            result
        }.onFailure { error ->
            Log.e(
                LOG_TAG,
                "Unable to read preferences: ${
                    file.absolutePath
                }",
                error
            )
        }.getOrNull()
    }

    private fun writeValues(
        snapshot: Map<String, Any>
    ): Boolean {
        val parentDirectory =
            file.parentFile
                ?: return false

        if (
            !parentDirectory.isDirectory &&
            !parentDirectory.mkdirs()
        ) {
            return false
        }

        val root =
            JSONObject()

        root.put(
            VERSION_KEY,
            FILE_VERSION
        )

        val entries =
            JSONObject()

        snapshot
            .toSortedMap()
            .forEach { (key, value) ->
                val encodedValue =
                    encodeValue(value)
                        ?: return@forEach

                entries.put(
                    key,
                    encodedValue
                )
            }

        root.put(
            VALUES_KEY,
            entries
        )

        val atomicFile =
            AtomicFile(file)

        var outputStream:
            FileOutputStream? = null

        return try {
            val activeOutputStream =
                atomicFile.startWrite()

            outputStream =
                activeOutputStream

            activeOutputStream.write(
                root
                    .toString(2)
                    .toByteArray(
                        Charsets.UTF_8
                    )
            )

            activeOutputStream.flush()

            atomicFile.finishWrite(
                activeOutputStream
            )

            outputStream = null

            true
        } catch (error: Exception) {
            outputStream?.let { stream ->
                atomicFile.failWrite(
                    stream
                )
            }

            Log.e(
                LOG_TAG,
                "Unable to write preferences: ${
                    file.absolutePath
                }",
                error
            )

            false
        }
    }

    private fun commitChanges(
        clearRequested: Boolean,
        updates: Map<String, Any>
    ): Boolean {
        return synchronized(lock) {
            val updatedValues =
                LinkedHashMap(values)

            if (clearRequested) {
                updatedValues.clear()
            }

            updates.forEach {
                    (key, value) ->
                if (value === RemovedValue) {
                    updatedValues.remove(key)
                } else {
                    updatedValues[key] =
                        copyValue(value)
                }
            }

            if (
                updatedValues == values &&
                file.isFile
            ) {
                return@synchronized true
            }

            if (!writeValues(updatedValues)) {
                return@synchronized false
            }

            values =
                updatedValues

            true
        }
    }

    private fun normalizeValues(
        source: Map<String, *>
    ): MutableMap<String, Any> {
        val normalized =
            linkedMapOf<String, Any>()

        source.forEach { (key, value) ->
            normalizeValue(value)
                ?.let { normalizedValue ->
                    normalized[key] =
                        normalizedValue
                }
        }

        return normalized
    }

    private fun normalizeValue(
        value: Any?
    ): Any? {
        return when (value) {
            is Boolean ->
                value

            is Float ->
                value

            is Int ->
                value

            is Long ->
                value

            is String ->
                value

            is Set<*> ->
                value
                    .filterIsInstance<String>()
                    .toSet()

            else ->
                null
        }
    }

    private fun copyValue(
        value: Any
    ): Any {
        return if (value is Set<*>) {
            value
                .filterIsInstance<String>()
                .toSet()
        } else {
            value
        }
    }

    private fun encodeValue(
        value: Any
    ): JSONObject? {
        val encoded =
            JSONObject()

        when (value) {
            is Boolean -> {
                encoded.put(
                    TYPE_KEY,
                    TYPE_BOOLEAN
                )

                encoded.put(
                    VALUE_KEY,
                    value
                )
            }

            is Float -> {
                encoded.put(
                    TYPE_KEY,
                    TYPE_FLOAT
                )

                encoded.put(
                    VALUE_KEY,
                    value.toDouble()
                )
            }

            is Int -> {
                encoded.put(
                    TYPE_KEY,
                    TYPE_INT
                )

                encoded.put(
                    VALUE_KEY,
                    value
                )
            }

            is Long -> {
                encoded.put(
                    TYPE_KEY,
                    TYPE_LONG
                )

                encoded.put(
                    VALUE_KEY,
                    value
                )
            }

            is String -> {
                encoded.put(
                    TYPE_KEY,
                    TYPE_STRING
                )

                encoded.put(
                    VALUE_KEY,
                    value
                )
            }

            is Set<*> -> {
                encoded.put(
                    TYPE_KEY,
                    TYPE_STRING_SET
                )

                val strings =
                    value
                        .filterIsInstance<String>()
                        .sorted()

                encoded.put(
                    VALUE_KEY,
                    JSONArray(strings)
                )
            }

            else ->
                return null
        }

        return encoded
    }

    private fun decodeValue(
        encoded: JSONObject
    ): Any? {
        return when (
            encoded.optString(
                TYPE_KEY
            )
        ) {
            TYPE_BOOLEAN ->
                encoded.getBoolean(
                    VALUE_KEY
                )

            TYPE_FLOAT ->
                encoded.getDouble(
                    VALUE_KEY
                ).toFloat()

            TYPE_INT ->
                encoded.getInt(
                    VALUE_KEY
                )

            TYPE_LONG ->
                encoded.getLong(
                    VALUE_KEY
                )

            TYPE_STRING ->
                encoded.getString(
                    VALUE_KEY
                )

            TYPE_STRING_SET -> {
                val array =
                    encoded.optJSONArray(
                        VALUE_KEY
                    ) ?: return emptySet<String>()

                val values =
                    linkedSetOf<String>()

                for (
                    index in
                        0 until array.length()
                ) {
                    values +=
                        array.getString(
                            index
                        )
                }

                values
            }

            else ->
                null
        }
    }

    private inner class EditorImpl :
        DingooPreferences.Editor {
        private val updates =
            linkedMapOf<String, Any>()

        private var clearRequested =
            false

        override fun putBoolean(
            key: String,
            value: Boolean
        ): DingooPreferences.Editor {
            updates[key] =
                value

            return this
        }

        override fun putFloat(
            key: String,
            value: Float
        ): DingooPreferences.Editor {
            updates[key] =
                value

            return this
        }

        override fun putInt(
            key: String,
            value: Int
        ): DingooPreferences.Editor {
            updates[key] =
                value

            return this
        }

        override fun putLong(
            key: String,
            value: Long
        ): DingooPreferences.Editor {
            updates[key] =
                value

            return this
        }

        override fun putString(
            key: String,
            value: String?
        ): DingooPreferences.Editor {
            updates[key] =
                value
                    ?: RemovedValue

            return this
        }

        override fun putStringSet(
            key: String,
            value: Set<String>?
        ): DingooPreferences.Editor {
            updates[key] =
                value?.toSet()
                    ?: RemovedValue

            return this
        }

        override fun remove(
            key: String
        ): DingooPreferences.Editor {
            updates[key] =
                RemovedValue

            return this
        }

        override fun clear():
            DingooPreferences.Editor {
            clearRequested =
                true

            return this
        }

        override fun commit(): Boolean {
            return commitChanges(
                clearRequested =
                    clearRequested,
                updates =
                    updates.toMap()
            )
        }

        override fun apply() {
            commit()
        }
    }

    internal companion object {
        private const val LOG_TAG =
            "DingooEmuStorage"

        private const val FILE_VERSION =
            1

        private const val VERSION_KEY =
            "version"

        private const val VALUES_KEY =
            "values"

        private const val TYPE_KEY =
            "type"

        private const val VALUE_KEY =
            "value"

        private const val TYPE_BOOLEAN =
            "boolean"

        private const val TYPE_FLOAT =
            "float"

        private const val TYPE_INT =
            "int"

        private const val TYPE_LONG =
            "long"

        private const val TYPE_STRING =
            "string"

        private const val TYPE_STRING_SET =
            "string_set"

        private val stores =
            ConcurrentHashMap<
                String,
                DingooFilePreferences
            >()

        fun open(
            file: File,
            legacyPreferences:
                SharedPreferences? = null
        ): DingooPreferences {
            return stores.getOrPut(
                file.absolutePath
            ) {
                DingooFilePreferences(
                    file = file,
                    legacyPreferences =
                        legacyPreferences
                )
            }
        }
    }

    private object RemovedValue
}
