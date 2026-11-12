package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Client
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients WHERE userId = :userId ORDER BY clientName COLLATE NOCASE ASC")
    fun getAllClients(userId: String): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE userId = :userId AND isActive = 1 ORDER BY clientName COLLATE NOCASE ASC")
    fun getActiveClients(userId: String): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getClientById(id: String): Client?

    @Query("SELECT * FROM clients WHERE userId = :userId AND LOWER(clientName) = LOWER(:name) LIMIT 1")
    suspend fun getClientByName(userId: String, name: String): Client?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<Client>)

    @Update
    suspend fun updateClient(client: Client)

    @Query("SELECT COUNT(*) FROM daily_report_entries WHERE clientId = :clientId")
    suspend fun countEntriesForClient(clientId: String): Int

    @Query("DELETE FROM clients WHERE id = :clientId")
    suspend fun deleteClient(clientId: String)
}
