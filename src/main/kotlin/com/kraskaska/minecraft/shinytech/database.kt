package com.kraskaska.minecraft.shinytech

import java.io.*
import java.util.UUID

// DATABASE INTERFACES
/**
 * Responsible for communicating between my back and databases
 */
interface DatabaseHandler {
    companion object {
        private var _dbHandle: DatabaseHandler? = File("shinytech.db").run {
            if (exists())
                HomebrewDatabaseHandler.Serializer.deserialize(
                    BufferedInputStream(
                        FileInputStream(
                            this
                        )
                    ).readAllBytes()
                )
            else HomebrewDatabaseHandler()
        }

        operator fun invoke(): DatabaseHandler {
            return _dbHandle!!
        }

        fun setDatabase(db: DatabaseHandler) {
            _dbHandle = db
        }

        fun saveDatabase() {
            when (_dbHandle) {
                is HomebrewDatabaseHandler -> {
                    val toWrite = HomebrewDatabaseHandler.Serializer.serialize(_dbHandle as HomebrewDatabaseHandler)
                    BufferedOutputStream(FileOutputStream(File("shinytech.db"))).apply {
                        write(toWrite)
                        close()
                    }
                }

                else -> throw IllegalArgumentException("Cannot save that database - do it manually.")
            }
        }
    }

    val shinyPassport: CRUDInterface<UUID, ShinyPassport>
    val shinyBank: ShinyBankHandler
}

interface CRUDInterface<K, V> : Iterable<Pair<K, V>> {
    fun create(value: V): K
    operator fun get(key: K): V
    fun getOrNull(key: K): V?
    operator fun set(key: K, value: V)
    fun update(key: K, value: V) {
        this[key] = value
    }

    fun delete(uuid: K)
}

interface ShinyBankHandler : Iterable<ShinyBankTransaction> {
    companion object {
        val DEPOSIT_WITHDRAWAL_UUID = UUID(0, 0)
    }

    operator fun get(uuid: UUID): Long
    fun transaction(transaction: ShinyBankTransaction)
    fun transaction(from: UUID, to: UUID, amount: Long) = transaction(ShinyBankTransaction(from, to, amount))
}

// DATA
data class ShinyPassport(val minecraftUUID: UUID? = null, val minecraftName: String? = null, val discordId: Long)

data class ShinyBankTransaction(val from: UUID, val to: UUID, val amount: Long)

// DATABASE IMPLEMENTATIONS
class HomebrewDatabaseHandler : DatabaseHandler {
    override val shinyPassport: CRUDInterface<UUID, ShinyPassport> = object : CRUDInterface<UUID, ShinyPassport> {
        val users = mutableMapOf<UUID, ShinyPassport>()
        override fun create(value: ShinyPassport): UUID {
            val uuid = UUID.randomUUID()
            users[uuid] = value
            return uuid
        }

        override fun get(key: UUID): ShinyPassport =
            users[key] ?: throw NoSuchElementException("User with that UUID doesn't exist")

        override fun getOrNull(key: UUID): ShinyPassport? = users[key]


        override fun delete(uuid: UUID) {
            users.remove(uuid)
        }

        override fun set(key: UUID, value: ShinyPassport) {
            users[key] = value
        }

        override fun iterator(): Iterator<Pair<UUID, ShinyPassport>> = users.map { it.toPair() }.iterator()
    }
    override val shinyBank: ShinyBankHandler = object : ShinyBankHandler {
        val transactions = mutableListOf<ShinyBankTransaction>()
        override fun get(uuid: UUID): Long = if (transactions.any { it.from == uuid || it.to == uuid })
            transactions.filter { it.from == uuid || it.to == uuid }.fold(0L) { acc, transaction ->
                if (transaction.to == uuid) acc + transaction.amount else if (transaction.from == uuid) acc - transaction.amount else acc
            }
        else 0

        override fun transaction(transaction: ShinyBankTransaction) {
            transactions.add(transaction)
        }

        override fun iterator(): Iterator<ShinyBankTransaction> = transactions.iterator()
    }

    object Serializer {
        val MAGIC = "SHINYTECH"
        val VERSION = 1
        fun serialize(db: HomebrewDatabaseHandler): ByteArray {
            val byteArrayStream = ByteArrayOutputStream()
            val stream = DataOutputStream(byteArrayStream)
            // Header
            stream.writeString(MAGIC)
            stream.writeInt(VERSION)

            // ShinyPassport
            stream.writeInt(db.shinyPassport.toList().size)
            for (shinyPassport in db.shinyPassport) {
                // uuid
                stream.writeString(shinyPassport.first.toString())
                // minecraft uuid
                stream.writeString(shinyPassport.second.minecraftUUID?.toString())
                // minecraft name
                stream.writeString(shinyPassport.second.minecraftName)
                // discord id
                stream.writeLong(shinyPassport.second.discordId)
            }
            stream.writeInt(db.shinyBank.toList().size)
            for (transaction in db.shinyBank) {
                // from
                stream.writeString(transaction.from.toString())
                // to
                stream.writeString(transaction.to.toString())
                // amount
                stream.writeLong(transaction.amount)
            }
            return byteArrayStream.toByteArray()
        }

        fun deserialize(byteArray: ByteArray): HomebrewDatabaseHandler {
            val stream = DataInputStream(ByteArrayInputStream(byteArray))
            val database = HomebrewDatabaseHandler()

            val magic = stream.readString()
            if (magic != MAGIC) {
                throw IllegalArgumentException("Not a ShinyTech Database or database is corrupted")
            }
            val version = stream.readInt()
            if (version != VERSION) {
                throw IllegalArgumentException("Database version is $version, yet serializer only supports $VERSION")
            }
            val shinyPassportAmount = stream.readInt()
            for (i in 0 until shinyPassportAmount) {
                val uuid = UUID.fromString(stream.readString())
                val minecraftUUID = stream.readString().ifEmpty { null }?.let { UUID.fromString(it) }
                val minecraftName = stream.readString().ifEmpty { null }
                val discordId = stream.readLong()
                database.shinyPassport[uuid] = ShinyPassport(minecraftUUID, minecraftName, discordId)
            }
            val shinyBankTransactionsAmount = stream.readInt()
            for (i in 0 until shinyBankTransactionsAmount) {
                val from = UUID.fromString(stream.readString())
                val to = UUID.fromString(stream.readString())
                val amount = stream.readLong()
                database.shinyBank.transaction(ShinyBankTransaction(from, to, amount))
            }
            return database
        }
    }
}

fun DataOutputStream.writeString(str: String?) {
    writeInt(str?.length ?: 0)
    if (!str.isNullOrEmpty()) {
        writeChars(str)
    }
}

fun DataInputStream.readChars(length: Int): String {
    val builder = StringBuilder()
    for (i in 0 until length) {
        builder.append(readChar())
    }
    return builder.toString()
}

fun DataInputStream.readString(): String = readChars(readInt())