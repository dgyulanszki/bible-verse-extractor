package hu.szegedibibliaszol.app.repository;

import hu.szegedibibliaszol.app.entity.Verse;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VersesRepository extends JpaRepository<Verse, Long> {

    @Query(value = """
            select translation
            from verses
            group by translation
            order by min(id)
            """, nativeQuery = true)
    List<String> findTranslations();

    @Query(value = """
            select book
            from verses
            where translation = :translation
            group by book
            order by min(id)
            """, nativeQuery = true)
    List<String> findBooksByTranslation(@Param("translation") String translation);

    @Query(value = """
            select chapter
            from verses
            where translation = :translation and book = :book
            group by chapter
            order by chapter
            """, nativeQuery = true)
    List<Integer> findChaptersByTranslationAndBook(
            @Param("translation") String translation,
            @Param("book") String book
    );

    @Query(value = """
            select verse
            from verses
            where translation = :translation and book = :book and chapter = :chapter
            group by verse
            order by verse
            """, nativeQuery = true)
    List<Integer> findVersesByTranslationAndBookAndChapter(
            @Param("translation") String translation,
            @Param("book") String book,
            @Param("chapter") Integer chapter
    );

    List<Verse> findByTranslationAndBookAndChapterOrderByVerseAsc(String translation, String book, Integer chapter);

    List<Verse> findByTranslationAndBookAndChapterAndVerseOrderByVerseAsc(
            String translation,
            String book,
            Integer chapter,
            Integer verse
    );
}

