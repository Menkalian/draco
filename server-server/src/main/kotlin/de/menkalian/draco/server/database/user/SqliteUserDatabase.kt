package de.menkalian.draco.server.database.user

import de.menkalian.draco.data.user.EntitledUser
import de.menkalian.draco.data.user.UserRight
import de.menkalian.draco.server.database.shared.MetaDataAwareDatabaseExtension
import de.menkalian.draco.server.database.user.dao.RightData
import de.menkalian.draco.server.database.user.dao.RightData.RightDataEntry.Companion.findDao
import de.menkalian.draco.server.database.user.dao.UserData
import de.menkalian.draco.server.database.user.dao.UserRightRelation
import de.menkalian.draco.server.util.initEnumDatabase
import de.menkalian.draco.server.util.logger
import de.menkalian.draco.server.util.sha512
import org.apache.tomcat.util.codec.binary.Base64
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Component
import java.io.File
import kotlin.random.Random


@Component
@Suppress("LeakingThis")
class SqliteUserDatabase(
    @Value("\${draco.sqlite.database.path}") private val databaseFolderPath: String,
    build: BuildProperties
) : IUserDatabase {
    companion object {
        private const val DATABASE_SCHEMA_VERSION = 1
    }

    override var isOpen: Boolean = false
    override val dbConnection: Database

    private val metadataExtension = MetaDataAwareDatabaseExtension()

    init {
        File(databaseFolderPath).mkdirs()

        dbConnection = Database.connect("jdbc:sqlite:$databaseFolderPath/users.db3", driver = "org.sqlite.JDBC")
        isOpen = true

        metadataExtension.initMetadata(this, build, DATABASE_SCHEMA_VERSION, "EntitledUsers")
        initRights()
        initAdminUser()
    }

    private fun initRights() {
        ensureOpen()
        initEnumDatabase(dbConnection, RightData, UserRight.values().map { it.name })
    }

    private fun initAdminUser() {
        ensureOpen()
        transaction(dbConnection) {
            SchemaUtils.create(UserData, RightData, UserRightRelation)

            val userExists = UserData.UserDataEntry.find { UserData.name.eq("admin") }.empty().not()
            if (!userExists) {
                val adminId = createUser("admin").id
                addRight(adminId, UserRight.ADMIN)
                logger().info("Created Admin user with ID $adminId")
            } else {
                val adminId = UserData.UserDataEntry.find { UserData.name.eq("admin") }.first().id
                addRight(adminId.value, UserRight.ADMIN)
            }
        }
    }

    override fun createUser(name: String): EntitledUser {
        ensureOpen()
        val accessToken = Random.nextBytes(128)
        File("$databaseFolderPath/$name.access.token")
            .writeText(Base64.encodeBase64String(accessToken))

        val hashedToken = accessToken.sha512()
        val accessHash = Base64.encodeBase64String(hashedToken)

        return transaction(dbConnection) {
            SchemaUtils.create(UserData)

            UserData
                .UserDataEntry
                .new {
                    this.name = name
                    this.accessHash = accessHash
                }.toUserObject()
        }
    }

    override fun getUsers(): List<EntitledUser> {
        ensureOpen()
        return transaction(dbConnection) {
            SchemaUtils.create(UserData, RightData, UserRightRelation)

            UserData.UserDataEntry.all()
                .map { it.toUserObject() }
                .toList()
        }
    }

    override fun getUser(id: Int): EntitledUser? {
        ensureOpen()
        return transaction(dbConnection) {
            SchemaUtils.create(UserData, RightData, UserRightRelation)

            UserData
                .UserDataEntry
                .findById(id)
                ?.toUserObject()
        }
    }

    override fun getUser(accessHash: String): EntitledUser? {
        ensureOpen()
        return transaction(dbConnection) {
            SchemaUtils.create(UserData, RightData, UserRightRelation)

            UserData
                .UserDataEntry
                .find { UserData.accessHash.eq(accessHash) }
                .firstOrNull()
                ?.toUserObject()
        }
    }

    override fun updateUser(userId: Int, modified: EntitledUser): EntitledUser? {
        ensureOpen()

        var result = true
        result = result && changeName(userId, modified.name) != null
        result = result && clearRights(userId) != null
        modified.rights.forEach {
            result = result && addRight(userId, it) != null
        }

        return getUser(userId)
    }

    override fun changeName(userId: Int, name: String): EntitledUser? {
        ensureOpen()
        return transaction(dbConnection) {
            SchemaUtils.create(UserData, RightData, UserRightRelation)

            UserData.UserDataEntry
                .findById(userId)
                ?.apply { this.name = name }
                ?.toUserObject()
        }
    }

    override fun addRight(userId: Int, right: UserRight): EntitledUser? {
        ensureOpen()
        return transaction(dbConnection) {
            SchemaUtils.create(UserData, RightData, UserRightRelation)

            UserData.UserDataEntry
                .findById(userId)
                ?.apply {
                    rights = SizedCollection(
                        (rights + right.findDao()).distinct()
                    )
                }
                ?.toUserObject()
        }
    }

    override fun clearRights(userId: Int): EntitledUser? {
        ensureOpen()
        return transaction(dbConnection) {
            SchemaUtils.create(UserData, RightData, UserRightRelation)

            UserData.UserDataEntry
                .findById(userId)
                ?.apply { rights = SizedCollection() }
                ?.toUserObject()
        }
    }

    override fun deleteUser(userId: Int): Boolean {
        ensureOpen()
        return transaction(dbConnection) {
            SchemaUtils.create(UserData, RightData, UserRightRelation)

            val u = UserData.UserDataEntry
                .findById(userId)
            u?.delete()

            u != null
        }
    }

    override fun close() {
        isOpen = false
        TransactionManager.closeAndUnregister(dbConnection)
    }
}