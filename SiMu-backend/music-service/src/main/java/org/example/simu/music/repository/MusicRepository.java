package org.example.simu.music.repository;

import org.example.simu.music.entity.Music;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MusicRepository extends JpaRepository<Music, Long> {

    /**
     * Keyset pagination over the id sequence: no OFFSET, so the query cost does not grow with
     * the page number and the index is used as-is.
     */
    @Query("select m from Music m where (:cursor is null or m.id > :cursor) order by m.id asc")
    Slice<Music> findPage(@Param("cursor") Long cursor, Pageable pageable);

    @Query("""
            select m from Music m
            where (:cursor is null or m.id > :cursor)
              and (lower(m.name) like lower(concat('%', :q, '%'))
                or lower(m.artist) like lower(concat('%', :q, '%')))
            order by m.id asc
            """)
    Slice<Music> search(@Param("q") String q, @Param("cursor") Long cursor, Pageable pageable);
}
