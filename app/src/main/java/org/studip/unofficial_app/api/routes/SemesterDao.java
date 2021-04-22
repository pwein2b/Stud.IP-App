package org.studip.unofficial_app.api.routes;
import androidx.room.Dao;
import androidx.room.Query;
import org.studip.unofficial_app.api.rest.StudipSemester;
import org.studip.unofficial_app.model.room.BasicDao;
@Dao
public interface SemesterDao extends BasicDao<StudipSemester>
{
    @Query("SELECT * FROM semesters")
    StudipSemester[] getAll();

    @Query("SELECT * FROM semesters WHERE id = :id")
    StudipSemester get(String id);


    @Query("SELECT * FROM semesters WHERE `begin` >= :time AND `end` <= :time")
    StudipSemester getByUnixTime(String time);
}
