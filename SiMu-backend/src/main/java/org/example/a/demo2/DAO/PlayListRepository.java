package org.example.a.demo2.DAO;

import org.example.a.demo2.entity.PlayList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayListRepository extends JpaRepository<PlayList, Long> {
}