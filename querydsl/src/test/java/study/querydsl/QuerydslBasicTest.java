package study.querydsl;

import static com.querydsl.core.types.dsl.Expressions.*;
import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import java.util.List;
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

  @PersistenceUnit
  EntityManagerFactory emf;

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
   * JPQL vs QueryDSL - JPQL은 실행 시점에 오류 생길 수 있음. vs QueryDSL은 컴파일 시점에 오류를 잡을 수 있음. - JPQL은 바인딩을 직접 해야 하는데, QueryDSL은
   * 파라미터 바인딩을 자동 처리함.
   */

  @Test
  @DisplayName("Querydsl로 Member 찾기")
  public void Querydsl로_Member_찾기() throws Exception {
    QMember m = new QMember("m"); // m은 어떤 QMember인지 구분하는 것. - 별칭 직접 지정
    QMember m2 = member; // 기본 인스턴스 사용

    Member findMember = queryFactory // QueryDSL은 JPQL의 빌더이다.
        .select(m)
        .from(m)
        .where(m.username.eq("member1")) // QueryDSL은 파라미터 바인딩을 자동 처리함. - db 성능도 좋아짐
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  @DisplayName("기본 인스턴스를 static import와 함께 사용") // 같은 테이블 조인하는 경우 아니면 기본 인스턴스 사용 권장
  public void 기본_인스턴스를_static_import와_함께_사용() throws Exception {
    Member findMember = queryFactory
        .select(member)
        .from(member)
        .where(member.username.eq("member1"))
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  @DisplayName("검색 쿼리")
  public void 검색_쿼리() throws Exception {
    Member findMember = queryFactory
        .selectFrom(member) // select와 from을 selectFrom으로 합칠 수 있음
        .where(member.username.eq("member1")
            .and(member.age.eq(10))) // .and(), .or()을 메서드 체인으로 연결 가능
        .fetchOne();

    member.username.eq("member1"); // username = 'member1'
    member.username.ne("member1"); //username != 'member1'
    member.username.eq("member1").not(); // username != 'member1'
    member.username.isNotNull(); //이름이 is not null
    member.age.in(10, 20); // age in (10,20)
    member.age.notIn(10, 20); // age not in (10, 20)
    member.age.between(10, 30); //between 10, 30
    member.age.goe(30); // age >= 30
    member.age.gt(30); // age > 30
    member.age.loe(30); // age <= 30
    member.age.lt(30); // age < 30
    member.username.like("member%"); //like 검색
    member.username.contains("member"); // like ‘%member%’ 검색
    member.username.startsWith("member"); //like ‘member%’ 검색

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  @DisplayName("AND 조건을 파라미터로 처리")
  public void AND_조건을_파라미터로_처리() throws Exception {
    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.username.eq("member1"), member.age.eq(10))
        // where에 파라미터로 검색 조건을 주면 and 조건이 추가됨
        // null 값이 무시됨. 메서드 추출을 사용하여 동적 쿼리를 깔끔하게 만들 수 있음.
        .fetch();

    assertThat(result.size()).isEqualTo(1);
  }

  @Test
  @DisplayName("결과 조회")
  public void 결과_조회() throws Exception {
    List<Member> fetch = queryFactory
        .selectFrom(member)
        .fetch(); // 리스트 조회. 데이터 없으면 빈 리스트 반환

   /* Member findMember1 = queryFactory
        .selectFrom(member)
        .fetchOne(); // 단 건 조회. 결과 없으면 null. 결과 둘 이상이면 com.querydsl.core.NonUniqueResultException
        */

    Member findMember2 = queryFactory
        .selectFrom(member)
        .fetchFirst(); // limit(1).fetchOne() , 처음 한 건 조회

    QueryResults<Member> memberQueryResults = queryFactory
        .selectFrom(member)
        .fetchResults(); // 페이징에서 사용. total count 쿼리 추가로 실행

    long count = queryFactory
        .selectFrom(member)
        .fetchCount(); // count 쿼리로 변경하여 count 수 조회
  }

  @Test
  @DisplayName("정렬")
  public void 정렬() throws Exception {
    em.persist(new Member(null, 100));
    em.persist(new Member("member5", 100));
    em.persist(new Member("member6", 100));

    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.eq(100))
        .orderBy(member.age.desc(), // 나이 내림차순
            member.username.asc() // 이름 올림차순
                .nullsLast() // 단 회원 이름이 없으면 마지막에 출력 (null 데이터 순서 부여)
            // .nullsFirst()도 있다.
        )
        .fetch();

    Member member5 = result.get(0);
    Member member6 = result.get(1);
    Member memberNull = result.get(2);
    assertThat(member5.getUsername()).isEqualTo("member5");
    assertThat(member6.getUsername()).isEqualTo("member6");
    assertThat(memberNull.getUsername()).isNull();
  }

  @Test
  @DisplayName("페이징 - 조회 건수 제한")
  public void 페이징_조회_건수_제한() throws Exception {
    //given
    List<Member> result = queryFactory
        .selectFrom(member)
        .orderBy(member.username.desc())
        .offset(1) // 0부터 시작
        .limit(2) // 최대 2건 조회
        .fetch();

    assertThat(result.size()).isEqualTo(2);
  }

  @Test
  @DisplayName("페이징 - 전체 조회수 필요") // 주의 - count쿼리 실행됨. 성능 주의
  public void 페이징_전체_조회수_필요() throws Exception {
    QueryResults<Member> queryResults = queryFactory
        .selectFrom(member)
        .orderBy(member.username.desc())
        .offset(1)
        .limit(2)
        .fetchResults();

    assertThat(queryResults.getTotal()).isEqualTo(4);
    assertThat(queryResults.getLimit()).isEqualTo(2);
    assertThat(queryResults.getOffset()).isEqualTo(1);
    assertThat(queryResults.getResults().size()).isEqualTo(2);
  }

  @Test
  @DisplayName("집합 함수")
  public void 집합_함수() throws Exception {
    List<Tuple> result = queryFactory
        .select(member.count(), member.age.sum(), member.age.avg(), member.age.max(), member.age.min())
        // JPQL이 제공하는 모든 집합 함수를 제공한다.
        .from(member)
        .fetch();

    Tuple tuple = result.get(0);

    assertThat(tuple.get(member.count())).isEqualTo(4);
    assertThat(tuple.get(member.age.sum())).isEqualTo(100);
    assertThat(tuple.get(member.age.avg())).isEqualTo(25);
    assertThat(tuple.get(member.age.max())).isEqualTo(40);
    assertThat(tuple.get(member.age.min())).isEqualTo(10);
  }

  @Test
  @DisplayName("GroupBy 사용")
  public void GroupBy_사용() throws Exception {
    // 각 팀의 평균 연령 구하기
    List<Tuple> result = queryFactory
        .select(team.name, member.age.avg())
        .from(member)
        .join(member.team, team)
        .groupBy(team.name) // 그룹화 된 결과를 제한하려면 having
        .having(team.name.ne("예외 팀"))
        .fetch();

    Tuple teamA = result.get(0);
    Tuple teamB = result.get(1);

    assertThat(teamA.get(team.name)).isEqualTo("teamA");
    assertThat(teamA.get(member.age.avg())).isEqualTo(15);

    assertThat(teamB.get(team.name)).isEqualTo("teamB");
    assertThat(teamB.get(member.age.avg())).isEqualTo(35);
  }

  /**
   * 팀 A에 소속된 모든 회원 찾기!
   */
  @Test
  @DisplayName("기본 조인")
  public void 기본_조인() throws Exception {
    // join(조인 대상, 별칭으로 사용할 Q타입) - 기본 문법
    List<Member> result = queryFactory
        .selectFrom(member)
        .join(member.team, team) // inner join
        // .innerJoin();
        // .leftJoin() // left outer join
        // .rightJoin() // right outer join
        .where(team.name.eq("teamA"))
        .fetch();

    assertThat(result)
        .extracting("username")
        .containsExactly("member1", "member2");
  }

  /**
   * 세타 조인 - 연관관계가 없는 필드로 조인 / 회원의 이름이 팀 이름과 같은 회원 조회
   */
  @Test
  @DisplayName("세타 조인")
  public void 세타_조인() throws Exception {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));

    List<Member> result = queryFactory
        .select(member)
        .from(member, team) // 여러 엔티티를 선택하여 세타 조인 / but 외부 조인 불가능
        .where(member.username.eq(team.name))
        .fetch();

    assertThat(result)
        .extracting("username")
        .containsExactly("teamA", "teamB");
  }

  /**
   * 1. 조인 대상 필터링 2. 연관관계 없는 엔티티 외부 조인 - 보통 이걸 위해서 사용
   */
  @Test
  @DisplayName("Join on 절")
  public void Join_on_절() throws Exception {
    // 회원과 팀을 조회하면서 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
    List<Tuple> result = queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(member.team, team) // ID 매칭
        .on(team.name.eq("teamA")) // left join 이면 on 절
        // on 절은 정말 외부 조인이 필요한 경우에 사용.
        .fetch();

    List<Tuple> result2 = queryFactory
        .select(member, team)
        .from(member)
        .innerJoin(member.team, team)
        .where(team.name.eq("teamA")) // inner join일 때는 익숙한 where 절 사용
        .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  @Test
  @DisplayName("연관관계 없는 엔티티 외부 조인")
  public void 연관관계_없는_엔티티_외부_조인() throws Exception {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));

    queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(team) // 일반 조인과 다르게 엔티티 하나만 들어간다. ID 매칭 X
        .on(member.username.eq(team.name)) // 하이버네이트 5.1부터 On 사용해서 서로 관계 없는 필드로 외부 조인하는 기능이 추가
        .fetch();
  }

  @Test
  @DisplayName("페치 조인 미적용")
  public void 페치_조인_미적용() throws Exception {
    em.flush();
    em.clear(); // 날린 후 확인

    Member findMember = queryFactory
        .selectFrom(member)
        .where(member.username.eq("member1"))
        // Lazy 로딩이니, db에서 조회될 때 멤버만 조회되고 팀은 조회되지 않는다
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // 초기화 되었는지 안되었는지 가르쳐줌.
    assertThat(loaded).as("페치 조인 미적용").isFalse();
  }

  @Test
  @DisplayName("페치 조인 적용")
  public void 페치_조인_적용() throws Exception {
    em.flush();
    em.clear(); // 날린 후 확인

    Member findMember = queryFactory
        .selectFrom(member)
        .join(member.team, team).fetchJoin() // fetchJoin()
        .where(member.username.eq("member1"))
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // 초기화 되었는지 안되었는지 가르쳐줌.
    assertThat(loaded).as("페치 조인 미적용").isTrue();
  }

  /**
   * 나이가 가장 많은 회원 조회
   *
   * @throws Exception
   */
  @Test
  @DisplayName("서브 쿼리 eq 사용")
  public void 서브_쿼리_eq_사용() throws Exception {
    QMember memberSub = new QMember("memberSub");

    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.eq(
            // 다시 조회. 40이라는 값이 나옴.
            select(memberSub.age.max()) // 바깥에 있는 member와 겹치면 안됨.
                .from(memberSub)
        ))
        .fetch();

    assertThat(result).extracting("age")
        .containsExactly(40);
  }

  /**
   * 나이가 평균 나이 이상인 회원
   *
   * @throws Exception
   */
  @Test
  @DisplayName("서브 쿼리 goe 사용")
  public void 서브_쿼리_goe_사용() throws Exception {
    QMember memberSub = new QMember("memberSub");

    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.goe(
            select(memberSub.age.avg())
                .from(memberSub)
        ))
        .fetch();

    assertThat(result).extracting("age")
        .containsExactly(30, 40);
  }

  @Test
  @DisplayName("서브 쿼리 여러 건 처리 in 사용")
  public void 서브_쿼리_여러_건_처리_in_사용() throws Exception {
    QMember memberSub = new QMember("memberSub");

    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.in(
            select(memberSub.age)
                .from(memberSub)
                .where(memberSub.age.gt(10))
        ))
        .fetch();

    assertThat(result).extracting("age")
        .containsExactly(20, 30, 40);
  }

  @Test
  @DisplayName("select 절에 subquery")
  public void select_절에_subquery() throws Exception {
    QMember memberSub = new QMember("memberSub");

    List<Tuple> fetch = queryFactory
        .select(member.username,
            select(memberSub.age.avg())
                .from(memberSub)
        )
        // JPA, JPQL 서브쿼리는 select, where 절에서 모두 되나, from절에서는 되지 않음.

        // 해결 방안
        // 서브쿼리를 join으로 바꾼다.
        // 쿼리를 2번 분리해서 실행한다.
        // native SQL을 사용한다.
        .from(member)
        .fetch();

    for (Tuple tuple : fetch) {
      System.out.println("username = " + tuple.get(member.username));
      System.out.println("age = " + tuple.get(
          select(memberSub.age.avg()).from(memberSub)));
      tuple.get(select(memberSub.age.avg()).from(memberSub));
    }
  }

  @Test
  @DisplayName("case문 단순한 조건")
  public void case문_단순한_조건() throws Exception {
    List<String> result = queryFactory
        .select(member.age
            .when(10).then("열살")
            .when(20).then("스무살")
            .otherwise("기타"))
        .from(member)
        .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  @DisplayName("case문 복잡한 조건")
  public void case문_복잡한_조건() throws Exception {
    List<String> result = queryFactory
        .select(new CaseBuilder() // 복잡할 때 사용
            .when(member.age.between(0, 20)).then("0~20살")
            .when(member.age.between(21, 30)).then("21~30살")
            .otherwise("기타"))
        .from(member)
        .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  /**
   * DB에서는 최소한의 필터링, 그루핑만 하기. 데이터를 가져오는데 집중하기.
   */


  /**
   * 1. 0~30살이 아닌 회원을 가장 먼저 출력 / 2. 0~20살 회원 출력 / 3. 21~30살 회원 출력
   */
  @Test
  @DisplayName("orderBy에서 Case문 함께 사용하기 좋은 예제")
  public void orderBy에서_Case문_함께_사용하기_좋은_예제() throws Exception {
    NumberExpression<Integer> rankPath = new CaseBuilder()
        .when(member.age.between(0, 20)).then(2)
        .when(member.age.between(21, 30)).then(1)
        .otherwise(3); // 복잡한 조건을 변수로 선언.

    queryFactory
        .select(member.username, member.age, rankPath)
        .from(member)
        .orderBy(rankPath.desc())
        .fetch();
  }

  @Test
  @DisplayName("상수, 문자 더하기")
  public void 상수_문자_더하기() throws Exception {
    Tuple result = queryFactory
        .select(member.username,
            constant("A")) // 상수가 필요할 때
        .from(member)
        .fetchFirst(); // JPQL로 쿼리가 날라가진 않음. 결과만 return

    String s = queryFactory
        .select(member.username.concat("_")
            .concat(member.age.stringValue())) // 문자가 아닌 다른 타입들은 stringValue()로 문자로 변환이 가능하다.
        // Enum을 처리할 때 자주 사용한다.
        .from(member)
        .where(member.username.eq("member1"))
        .fetchOne();
    System.out.println("s = " + s);
  }
}
