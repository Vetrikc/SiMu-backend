package org.example.simu.playlist.repository;

import org.example.simu.playlist.entity.UserPlaylist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPlaylistRepository extends JpaRepository<UserPlaylist, Long> {

    List<UserPlaylist> findByUserIdOrderByIdAsc(Long userId);

    List<UserPlaylist> findByPlaylistId(Long playlistId);

    boolean existsByUserIdAndPlaylistId(Long userId, Long playlistId);
}
