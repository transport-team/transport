package com.transport.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.transport.domain.Notice;

// 공지사항
public interface NoticeRepository extends JpaRepository<Notice, Long> {

	// 공지 목록: 제목 검색 (keyword가 null이면 전체), 작성자 이름까지 한 번에 가져온다
	@Query(value = "select n from Notice n join fetch n.writer"
			+ " where (:keyword is null or n.title like concat('%', :keyword, '%'))",
			countQuery = "select count(n) from Notice n"
			+ " where (:keyword is null or n.title like concat('%', :keyword, '%'))")
	Page<Notice> search(@Param("keyword") String keyword, Pageable pageable);
}
