package ru.itmo.secureapi.post;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select p from Post p join fetch p.author order by p.createdAt desc")
    List<Post> findAllWithAuthor();

    /** Поиск по подстроке: значение передаётся как параметр, а не склеивается со строкой запроса. */
    @Query("""
            select p from Post p join fetch p.author
            where lower(p.title) like lower(concat('%', :query, '%'))
               or lower(p.content) like lower(concat('%', :query, '%'))
            order by p.createdAt desc
            """)
    List<Post> search(@Param("query") String query);
}
