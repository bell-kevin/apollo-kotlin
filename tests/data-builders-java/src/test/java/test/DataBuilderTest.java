package test;

import com.apollographql.apollo3.api.FakeResolver;
import com.apollographql.apollo3.api.FakeResolverContext;
import data.builders.GetAliasesQuery;
import data.builders.GetAnimalQuery;
import data.builders.GetCustomScalarQuery;
import data.builders.GetDirectionQuery;
import data.builders.GetEverythingQuery;
import data.builders.GetFelineQuery;
import data.builders.GetIntQuery;
import data.builders.GetPartialQuery;
import data.builders.PutIntMutation;
import data.builders.type.Direction;
import data.builders.type.builder.BuilderFactory;
import data.builders.MyLong;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class DataBuilderTest {
  private BuilderFactory factory = BuilderFactory.DEFAULT;

  @Test
  public void nullabilityTest() {
    GetIntQuery.Data data = GetIntQuery.buildData(
        factory.buildQuery()
            .nullableInt(null)
            .nonNullableInt(42)
            .build()
    );

    assertEquals(null, data.nullableInt);
    assertEquals(Integer.valueOf(42), data.nonNullableInt);
  }

  @Test
  public void aliasTest() {
    GetAliasesQuery.Data data = GetAliasesQuery.buildData(
        factory.buildQuery()
            .cat(factory.buildCat().species("Cat").build())
            .alias("aliasedNullableInt", 50)
            .alias(
                "aliasedCat",
                factory.buildCat().species("AliasedCat").build()
            )
            .build()
    );

    assertEquals(Integer.valueOf(50), data.aliasedNullableInt);
    assertEquals("Cat", data.cat.species);
    assertEquals("AliasedCat", data.aliasedCat.species);
  }

  @Test
  public void mutationTest() {
    PutIntMutation.Data data = PutIntMutation.buildData(
        factory.buildMutationRoot()
            .nullableInt(null)
            .build()
    );

    assertEquals(null, data.nullableInt);
  }

  @Test
  public void interfaceTest() {
    GetAnimalQuery.Data data = GetAnimalQuery.buildData(
        factory.buildQuery()
            .animal(
                factory.buildLion()
                    .species("LionSpecies")
                    .roar("Rooooaaarr")
                    .build()
            )
            .build()
    );

    assertEquals("Lion", data.animal.__typename);
    assertEquals("LionSpecies", data.animal.species);
    assertEquals("Rooooaaarr", data.animal.onLion.roar);
  }

  @Test
  public void unionTest1() {
    GetFelineQuery.Data data = GetFelineQuery.buildData(
        factory.buildQuery()
            .feline(
                factory.buildLion()
                    .species("LionSpecies")
                    .roar("Rooooaaarr")
                    .build()
            )
            .build()
    );

    assertEquals("Lion", data.feline.__typename);
    assertEquals(null, data.feline.onCat);
  }

  @Test
  public void unionTest2() {
    GetFelineQuery.Data data = GetFelineQuery.buildData(
        factory.buildQuery()
            .feline(
                factory.buildCat()
                    .species("CatSpecies")
                    .mustaches(5)
                    .build()
            )
            .build()
    );

    assertEquals("Cat", data.feline.__typename);
    assertEquals(Integer.valueOf(5), data.feline.onCat.mustaches);
  }

  @Test
  public void enumTest() {
    GetDirectionQuery.Data data = GetDirectionQuery.buildData(
        factory.buildQuery()
            .direction(Direction.NORTH)
            .build()
    );
    assertEquals(Direction.NORTH, data.direction);
  }

  @Test
  public void customScalarTest() {
    GetCustomScalarQuery.Data data = GetCustomScalarQuery.buildData(
        factory.buildQuery()
            .long1(new MyLong(42L))
            .long2(new MyLong(43L))
            .long3(44)
            .listOfListOfLong1(Collections.singletonList(Collections.singletonList(new MyLong(42L))))
            .build()
    );


    assertEquals(Long.valueOf(42), data.long1.value);
    assertEquals(Long.valueOf(43), data.long2.value);
    assertEquals(44, data.long3);
  }

  @Test
  public void fakeValues() {
    GetEverythingQuery.Data data = GetEverythingQuery.buildData(factory.buildQuery().build());

    assertEquals(Direction.SOUTH, data.direction);
    assertEquals(Integer.valueOf(0), data.nullableInt);
    assertEquals(Integer.valueOf(1), data.nonNullableInt);
    assertEquals(Arrays.asList(
        Arrays.asList(2, 3, 4),
        Arrays.asList(5, 6, 7),
        Arrays.asList(8, 9, 10)
    ), data.listOfListOfInt);
    assertEquals(Integer.valueOf(11), data.cat.mustaches);
    assertEquals("Lion", data.animal.__typename);
    assertEquals("Cat", data.feline.__typename);
  }

  @Test
  public void partialFakeValues() {
    GetPartialQuery.Data data = GetPartialQuery.buildData(
        factory.buildQuery()
            .listOfListOfAnimal(
                Collections.singletonList(
                    Collections.singletonList(
                        factory.buildLion()
                            .species("FooSpecies")
                            .build()
                    )
                )
            )
            .build()
      );


    assertEquals(
        new GetPartialQuery.Data(
            Collections.singletonList(
                Collections.singletonList (
                    new GetPartialQuery.ListOfListOfAnimal(
                        "Lion",
                        "FooSpecies",
                        new GetPartialQuery.OnLion("roar")
                    )
                )
            )
        ),
        data
    );
  }

  static class MyFakeResolver implements FakeResolver {
    @NotNull @Override public Object resolveLeaf(@NotNull FakeResolverContext context) {
      String name = context.getMergedField().getType().leafType().getName();
      Object ret = null;
      switch (name) {
        case "Long1": {
          ret = "45";
          break;
        }
        case "Long2": {
          ret = new MyLong(46L);
          break;
        }
        case "Long3": {
          ret = 47L;
          break;
        }
        default: throw new IllegalStateException();
      }
      return ret;
    }

    @Override public int resolveListSize(@NotNull FakeResolverContext context) {
      return 1;
    }

    @Override public boolean resolveMaybeNull(@NotNull FakeResolverContext context) {
      return false;
    }

    @NotNull @Override public String resolveTypename(@NotNull FakeResolverContext context) {
      throw new IllegalStateException();
    }
  }

  @Test
  public void customScalarFakeValues() {
    GetCustomScalarQuery.Data data = GetCustomScalarQuery.buildData(factory.buildQuery().build(), new MyFakeResolver());

    assertEquals(Long.valueOf(45L), data.long1.value);
    assertEquals(Long.valueOf(46L), data.long2.value);
    assertEquals(47, data.long3); // AnyAdapter will try to fit the smallest possible number
  }
}
