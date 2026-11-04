package com.tobiso.tobisoappnative.repository

import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.DutyOverview
import com.tobiso.tobisoappnative.model.DutyWeek
import com.tobiso.tobisoappnative.model.MyDutyPerson
import com.tobiso.tobisoappnative.model.SetMyDutyPersonRequest
import com.tobiso.tobisoappnative.model.JoinClassRequest
import com.tobiso.tobisoappnative.model.MyClass
import com.tobiso.tobisoappnative.model.SchoolClass
import com.tobiso.tobisoappnative.model.UpdateMyGroupsRequest
import retrofit2.HttpException
import java.io.IOException

/** User-facing failure of a classes/calendar call; [message] is Czech and safe to show. */
class ApiFailure(message: String, val code: Int? = null) : Exception(message)

fun mapApiError(e: Throwable): ApiFailure = when (e) {
    is ApiFailure -> e
    is HttpException -> when (e.code()) {
        401 -> ApiFailure("Přihlášení vypršelo. Přihlas se prosím znovu.", 401)
        403 -> ApiFailure("K tomu nemáš oprávnění.", 403)
        404 -> ApiFailure("Položka nebyla nalezena.", 404)
        429 -> ApiFailure("Příliš mnoho požadavků. Zkus to prosím později.", 429)
        else -> ApiFailure("Server vrátil chybu (${e.code()}).", e.code())
    }
    is IOException -> ApiFailure("Nepodařilo se spojit se serverem. Zkontroluj připojení.")
    else -> ApiFailure("Něco se pokazilo. Zkus to prosím znovu.")
}

interface ClassesRepository {
    suspend fun getClasses(): Result<List<SchoolClass>>
    /** Success(null) = the student has no membership (HTTP 204). */
    suspend fun getMyClass(): Result<MyClass?>
    suspend fun join(classId: Int, groupIds: List<Int>): Result<MyClass>
    suspend fun updateGroups(groupIds: List<Int>): Result<MyClass>
    suspend fun leave(): Result<Unit>
    /** Success(null) = no approved class (HTTP 204). */
    suspend fun getDutyOverview(): Result<DutyOverview?>
    suspend fun getDutyWeeks(): Result<List<DutyWeek>>
    suspend fun getMyDutyPerson(): Result<MyDutyPerson?>
    suspend fun setMyDutyPerson(personId: Int?): Result<MyDutyPerson>
}

class ClassesRepositoryImpl(private val api: ApiService) : ClassesRepository {

    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(mapApiError(e))
    }

    override suspend fun getClasses() = call { api.getClasses() }

    override suspend fun getMyClass() = call {
        val r = api.getMyClass()
        if (!r.isSuccessful) throw HttpException(r)
        r.body() // null on 204
    }

    override suspend fun join(classId: Int, groupIds: List<Int>) =
        call { api.joinClass(JoinClassRequest(classId, groupIds.ifEmpty { null })) }

    override suspend fun updateGroups(groupIds: List<Int>) =
        call { api.updateMyGroups(UpdateMyGroupsRequest(groupIds)) }

    override suspend fun leave() = call {
        val r = api.leaveClass()
        if (!r.isSuccessful) throw HttpException(r)
    }

    override suspend fun getDutyOverview() = call {
        val r = api.getDutyOverview()
        if (!r.isSuccessful) throw HttpException(r)
        r.body()
    }

    override suspend fun getDutyWeeks() = call {
        val r = api.getDutyWeeks()
        if (!r.isSuccessful) throw HttpException(r)
        r.body().orEmpty()
    }

    override suspend fun getMyDutyPerson() = call {
        val r = api.getMyDutyPerson()
        if (!r.isSuccessful) throw HttpException(r)
        r.body()
    }

    override suspend fun setMyDutyPerson(personId: Int?) = call { api.setMyDutyPerson(SetMyDutyPersonRequest(personId)) }
}
