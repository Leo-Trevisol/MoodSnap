package com.br.leo.moodsnap.service.repository

import android.content.Context
import com.br.leo.moodsnap.service.model.MoodModel
import com.br.leo.moodsnap.service.repository.database.MoodDatabase
import com.br.leo.moodsnap.ui.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.*

class MoodRepository(context: Context) {

    private val dataBase = MoodDatabase.getDatabase(context).moodDao()
    private val firebaseDB = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()

    fun get(id: Int): MoodModel {
        return dataBase.load(id)
    }

    fun save(mood: MoodModel): Boolean {
        val success = dataBase.save(mood) > 0
        if (success) {
            saveToFirebase(mood)
        }
        return success
    }

    private fun saveToFirebase(mood: MoodModel) {
        val user = firebaseAuth.currentUser ?: return

        val firebaseModel = hashMapOf(
            "date" to mood.date,
            "moodType" to mood.moodType,
            "description" to mood.description,
            "imagePath" to mood.imagePath
        )

        val docId = mood.date.time.toString() // ou use UUID.randomUUID().toString()

        firebaseDB.collection("users")
            .document(user.uid)
            .collection("moods")
            .document(docId)
            .set(firebaseModel, SetOptions.merge())
            .addOnSuccessListener {
                // Log.d("Firebase", "Salvo com sucesso para o usuário ${user.uid}")
            }
            .addOnFailureListener {
                // Log.e("Firebase", "Erro ao salvar no Firebase", it)
            }
    }

    fun getAll(): List<MoodModel> {
        return dataBase.getAll()
    }

    fun getMoodType(moodType: Int): List<MoodModel> {
        return dataBase.getMoodType(moodType)
    }

    fun update(mood: MoodModel): Boolean {
        val success = dataBase.update(mood) > 0
        if (success) {
            saveToFirebase(mood)
        }
        return success
    }

    fun delete(mood: MoodModel) {
        dataBase.delete(mood)
        deleteFromFirebase(mood)
    }

    private fun deleteFromFirebase(mood: MoodModel) {
        val user = firebaseAuth.currentUser ?: return
        val docId = mood.date.time.toString()

        firebaseDB.collection("users")
            .document(user.uid)
            .collection("moods")
            .document(docId)
            .delete()
            .addOnSuccessListener {
                // Log.d("Firebase", "Deletado com sucesso")
            }
            .addOnFailureListener {
                // Log.e("Firebase", "Erro ao deletar", it)
            }
    }

    fun getMoodByDate(date: Date): MoodModel? {
        val (startOfDay, endOfDay) = DateUtils.getStartAndEndOfDay(date)
        return dataBase.getMoodByDateRange(startOfDay, endOfDay)
    }
}
