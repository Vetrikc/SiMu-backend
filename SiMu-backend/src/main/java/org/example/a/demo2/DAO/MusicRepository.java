package org.example.a.demo2.DAO;

import org.example.a.demo2.entity.Music;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MusicRepository extends JpaRepository<Music, Long> {

}
