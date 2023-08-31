package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

  @PersistenceContext
  EntityManager em;

  JPAQueryFactory queryFactory; // JPAQueryFactory 생성

  @BeforeEach
  public void before() { // 예제 데이터
    queryFactory = new JPAQueryFactory(em); // 필드 레벨에 있어도 문제 X
    // 여러 스레드에서 동시에 같은 EM에 접근해도, 트랜잭션 마다 별도의 영속성 컨텍스트를 제공하기 때문에 동시성 문제를 걱정하지 않아도 됨.
    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");
    em.persist(teamA);
    em.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);
    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);
    em.persist(member1);
    em.persist(member2);
    em.persist(member3);
    em.persist(member4);
  }


  @Test
  @DisplayName("JPQL로 Member 찾기")
  public void JPQL로_Member_찾기() throws Exception {
    String jpqlString = "select m from Member m " +
        "where m.username = :username";

    Member findMember = em.createQuery(jpqlString, Member.class)
        .setParameter("username", "member1")
        .getSingleResult();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  /**
   * JPQL vs QueryDSL
   * - JPQL은 실행 시점에 오류 생길 수 있음. vs QueryDSL은 컴파일 시점에 오류를 잡을 수 있음.
   * - JPQL은 바인딩을 직접 해야 하는데, QueryDSL은 파라미터 바인딩을 자동 처리함.
   */

  @Test
  @DisplayName("Querydsl로 Member 찾기")
  public void Querydsl로_Member_찾기() throws Exception {
    QMember m = new QMember("m"); // m은 어떤 QMember인지 구분하는 것.

    Member findMember = queryFactory // QueryDSL은 JPQL의 빌더이다.
        .select(m)
        .from(m)
        .where(m.username.eq("member1")) // QueryDSL은 파라미터 바인딩을 자동 처리함. - db 성능도 좋아짐
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }
}
