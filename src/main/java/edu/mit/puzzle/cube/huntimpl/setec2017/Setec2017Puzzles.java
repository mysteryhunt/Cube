package edu.mit.puzzle.cube.huntimpl.setec2017;

import com.google.common.collect.ImmutableMap;

import edu.mit.puzzle.cube.core.model.Puzzle;
import edu.mit.puzzle.cube.huntimpl.setec2017.Setec2017HuntDefinition.Character;
import edu.mit.puzzle.cube.huntimpl.setec2017.Setec2017HuntDefinition.Setec2017Puzzle;
import edu.mit.puzzle.cube.huntimpl.setec2017.Setec2017HuntDefinition.SolveReward;
import edu.mit.puzzle.cube.huntimpl.setec2017.Setec2017HuntDefinition.UnlockConstraint;

public class Setec2017Puzzles {
    private Setec2017Puzzles() {}

    static final ImmutableMap<String, Setec2017Puzzle> PUZZLES;
    static {
        ImmutableMap.Builder<String, Setec2017Puzzle> puzzlesBuilder = ImmutableMap.builder();

        puzzlesBuilder.put("fighter", Setec2017Puzzle.create(
                Puzzle.create("fighter", "FIGHTERMETA"),
                UnlockConstraint.builder().build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("f1", Setec2017Puzzle.create(
                Puzzle.create("f1", "FIGHTER1"),
                UnlockConstraint.builder().build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.FIGHTER, 1)
                        .setGold(1)
                        .build()
        ));
        puzzlesBuilder.put("f2", Setec2017Puzzle.create(
                Puzzle.create("f2", "FIGHTER2"),
                UnlockConstraint.builder().build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.FIGHTER, 1)
                        .build()
        ));
        puzzlesBuilder.put("f3", Setec2017Puzzle.create(
                Puzzle.create("f3", "FIGHTER3"),
                UnlockConstraint.builder()
                        .addSumConstraint(1, Character.FIGHTER)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.FIGHTER, 1)
                        .build()
        ));
        puzzlesBuilder.put("f4", Setec2017Puzzle.create(
                Puzzle.create("f4", "FIGHTER4"),
                UnlockConstraint.builder()
                        .addSumConstraint(2, Character.FIGHTER)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.FIGHTER, 1)
                        .build()
        ));
        puzzlesBuilder.put("f5", Setec2017Puzzle.create(
                Puzzle.create("f5", "FIGHTER5"),
                UnlockConstraint.builder()
                        .addSumConstraint(4, Character.FIGHTER)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.FIGHTER, 1)
                        .build()
        ));
        puzzlesBuilder.put("f6", Setec2017Puzzle.create(
                Puzzle.create("f6", "FIGHTER6"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.FIGHTER)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.FIGHTER, 1)
                        .build()
        ));
        puzzlesBuilder.put("f7", Setec2017Puzzle.create(
                Puzzle.create("f7", "FIGHTER7"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.FIGHTER)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.FIGHTER, 1)
                        .build()
        ));
        puzzlesBuilder.put("f8", Setec2017Puzzle.create(
                Puzzle.create("f8", "FIGHTER8"),
                UnlockConstraint.builder()
                        .addSumConstraint(7, Character.FIGHTER)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.FIGHTER, 1)
                        .build()
        ));
        puzzlesBuilder.put("f9", Setec2017Puzzle.create(
                Puzzle.create("f9", "FIGHTER9"),
                UnlockConstraint.builder()
                        .addSumConstraint(9, Character.FIGHTER)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.FIGHTER, 1)
                        .build()
        ));
        puzzlesBuilder.put("f10", Setec2017Puzzle.create(
                Puzzle.create("f10", "FIGHTER10"),
                UnlockConstraint.builder()
                        .addSumConstraint(11, Character.FIGHTER)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.FIGHTER, 1)
                        .build()
        ));
        puzzlesBuilder.put("f11", Setec2017Puzzle.create(
                Puzzle.create("f11", "FIGHTER11"),
                UnlockConstraint.builder()
                        .addSumConstraint(13, Character.FIGHTER)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.FIGHTER, 1)
                        .build()
        ));

        puzzlesBuilder.put("wizard", Setec2017Puzzle.create(
                Puzzle.create("wizard", "WIZARDMETA"),
                UnlockConstraint.builder().build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("w1", Setec2017Puzzle.create(
                Puzzle.create("w1", "WIZARD1"),
                UnlockConstraint.builder().build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.WIZARD, 1)
                        .build()
        ));
        puzzlesBuilder.put("w2", Setec2017Puzzle.create(
                Puzzle.create("w2", "WIZARD2"),
                UnlockConstraint.builder().build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.WIZARD, 1)
                        .build()
        ));
        puzzlesBuilder.put("w3", Setec2017Puzzle.create(
                Puzzle.create("w3", "WIZARD3"),
                UnlockConstraint.builder()
                        .addSumConstraint(1, Character.WIZARD)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.WIZARD, 1)
                        .build()
        ));
        puzzlesBuilder.put("w4", Setec2017Puzzle.create(
                Puzzle.create("w4", "WIZARD4"),
                UnlockConstraint.builder()
                        .addSumConstraint(2, Character.WIZARD)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.WIZARD, 1)
                        .build()
        ));
        puzzlesBuilder.put("w5", Setec2017Puzzle.create(
                Puzzle.create("w5", "WIZARD5"),
                UnlockConstraint.builder()
                        .addSumConstraint(4, Character.WIZARD)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.WIZARD, 1)
                        .build()
        ));
        puzzlesBuilder.put("w6", Setec2017Puzzle.create(
                Puzzle.create("w6", "WIZARD6"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.WIZARD)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.WIZARD, 1)
                        .build()
        ));
        puzzlesBuilder.put("w7", Setec2017Puzzle.create(
                Puzzle.create("w7", "WIZARD7"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.WIZARD)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.WIZARD, 1)
                        .build()
        ));
        puzzlesBuilder.put("w8", Setec2017Puzzle.create(
                Puzzle.create("w8", "WIZARD8"),
                UnlockConstraint.builder()
                        .addSumConstraint(7, Character.WIZARD)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.WIZARD, 1)
                        .build()
        ));
        puzzlesBuilder.put("w9", Setec2017Puzzle.create(
                Puzzle.create("w9", "WIZARD9"),
                UnlockConstraint.builder()
                        .addSumConstraint(9, Character.WIZARD)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.WIZARD, 1)
                        .build()
        ));
        puzzlesBuilder.put("w10", Setec2017Puzzle.create(
                Puzzle.create("w10", "WIZARD10"),
                UnlockConstraint.builder()
                        .addSumConstraint(11, Character.WIZARD)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.WIZARD, 1)
                        .build()
        ));
        puzzlesBuilder.put("w11", Setec2017Puzzle.create(
                Puzzle.create("w11", "WIZARD11"),
                UnlockConstraint.builder()
                        .addSumConstraint(13, Character.WIZARD)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.WIZARD, 1)
                        .build()
        ));

        puzzlesBuilder.put("cleric", Setec2017Puzzle.create(
                Puzzle.create("cleric", "CLERICMETA"),
                UnlockConstraint.builder().build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("cl1", Setec2017Puzzle.create(
                Puzzle.create("cl1", "CLERIC1"),
                UnlockConstraint.builder().build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CLERIC, 1)
                        .build()
        ));
        puzzlesBuilder.put("cl2", Setec2017Puzzle.create(
                Puzzle.create("cl2", "CLERIC2"),
                UnlockConstraint.builder().build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CLERIC, 1)
                        .build()
        ));
        puzzlesBuilder.put("cl3", Setec2017Puzzle.create(
                Puzzle.create("cl3", "CLERIC3"),
                UnlockConstraint.builder()
                        .addSumConstraint(1, Character.CLERIC)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CLERIC, 1)
                        .build()
        ));
        puzzlesBuilder.put("cl4", Setec2017Puzzle.create(
                Puzzle.create("cl4", "CLERIC4"),
                UnlockConstraint.builder()
                        .addSumConstraint(2, Character.CLERIC)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CLERIC, 1)
                        .build()
        ));
        puzzlesBuilder.put("cl5", Setec2017Puzzle.create(
                Puzzle.create("cl5", "CLERIC5"),
                UnlockConstraint.builder()
                        .addSumConstraint(4, Character.CLERIC)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CLERIC, 1)
                        .build()
        ));
        puzzlesBuilder.put("cl6", Setec2017Puzzle.create(
                Puzzle.create("cl6", "CLERIC6"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.CLERIC)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CLERIC, 1)
                        .build()
        ));
        puzzlesBuilder.put("cl7", Setec2017Puzzle.create(
                Puzzle.create("cl7", "CLERIC7"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.CLERIC)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CLERIC, 1)
                        .build()
        ));
        puzzlesBuilder.put("cl8", Setec2017Puzzle.create(
                Puzzle.create("cl8", "CLERIC8"),
                UnlockConstraint.builder()
                        .addSumConstraint(7, Character.CLERIC)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CLERIC, 1)
                        .build()
        ));
        puzzlesBuilder.put("cl9", Setec2017Puzzle.create(
                Puzzle.create("cl9", "CLERIC9"),
                UnlockConstraint.builder()
                        .addSumConstraint(10, Character.CLERIC)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CLERIC, 1)
                        .build()
        ));
        puzzlesBuilder.put("cl10", Setec2017Puzzle.create(
                Puzzle.create("cl10", "CLERIC10"),
                UnlockConstraint.builder()
                        .addSumConstraint(13, Character.CLERIC)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CLERIC, 1)
                        .build()
        ));

        puzzlesBuilder.put("linguist", Setec2017Puzzle.create(
                Puzzle.create("linguist", "LINGUISTMETA"),
                UnlockConstraint.builder()
                        .addSumConstraint(12, Character.values())
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("l1", Setec2017Puzzle.create(
                Puzzle.create("l1", "LINGUIST1"),
                UnlockConstraint.builder()
                        .addSumConstraint(12, Character.values())
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.LINGUIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("l2", Setec2017Puzzle.create(
                Puzzle.create("l2", "LINGUIST2"),
                UnlockConstraint.builder()
                        .addSumConstraint(12, Character.values())
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.LINGUIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("l3", Setec2017Puzzle.create(
                Puzzle.create("l3", "LINGUIST3"),
                UnlockConstraint.builder()
                        .addSumConstraint(12, Character.values())
                        .addSumConstraint(1, Character.LINGUIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.LINGUIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("l4", Setec2017Puzzle.create(
                Puzzle.create("l4", "LINGUIST4"),
                UnlockConstraint.builder()
                        .addSumConstraint(12, Character.values())
                        .addSumConstraint(3, Character.LINGUIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.LINGUIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("l5", Setec2017Puzzle.create(
                Puzzle.create("l5", "LINGUIST5"),
                UnlockConstraint.builder()
                        .addSumConstraint(12, Character.values())
                        .addSumConstraint(4, Character.LINGUIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.LINGUIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("l6", Setec2017Puzzle.create(
                Puzzle.create("l6", "LINGUIST6"),
                UnlockConstraint.builder()
                        .addSumConstraint(12, Character.values())
                        .addSumConstraint(7, Character.LINGUIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.LINGUIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("l7", Setec2017Puzzle.create(
                Puzzle.create("l7", "LINGUIST7"),
                UnlockConstraint.builder()
                        .addSumConstraint(12, Character.values())
                        .addSumConstraint(7, Character.LINGUIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.LINGUIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("l8", Setec2017Puzzle.create(
                Puzzle.create("l8", "LINGUIST8"),
                UnlockConstraint.builder()
                        .addSumConstraint(12, Character.values())
                        .addSumConstraint(8, Character.LINGUIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.LINGUIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("l9", Setec2017Puzzle.create(
                Puzzle.create("l9", "LINGUIST9"),
                UnlockConstraint.builder()
                        .addSumConstraint(12, Character.values())
                        .addSumConstraint(10, Character.LINGUIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.LINGUIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("l10", Setec2017Puzzle.create(
                Puzzle.create("l10", "LINGUIST10"),
                UnlockConstraint.builder()
                        .addSumConstraint(12, Character.values())
                        .addSumConstraint(12, Character.LINGUIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.LINGUIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("l11", Setec2017Puzzle.create(
                Puzzle.create("l11", "LINGUIST11"),
                UnlockConstraint.builder()
                        .addSumConstraint(12, Character.values())
                        .addSumConstraint(14, Character.LINGUIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.LINGUIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("l12", Setec2017Puzzle.create(
                Puzzle.create("l12", "LINGUIST12"),
                UnlockConstraint.builder()
                        .addSumConstraint(12, Character.values())
                        .addSumConstraint(15, Character.LINGUIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.LINGUIST, 1)
                        .build()
        ));

        puzzlesBuilder.put("economist", Setec2017Puzzle.create(
                Puzzle.create("economist", "ECONOMISTMETA"),
                UnlockConstraint.builder()
                        .addSumConstraint(24, Character.values())
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("e1", Setec2017Puzzle.create(
                Puzzle.create("e1", "ECONOMIST1"),
                UnlockConstraint.builder()
                        .addSumConstraint(24, Character.values())
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.ECONOMIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("e2", Setec2017Puzzle.create(
                Puzzle.create("e2", "ECONOMIST2"),
                UnlockConstraint.builder()
                        .addSumConstraint(24, Character.values())
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.ECONOMIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("e3", Setec2017Puzzle.create(
                Puzzle.create("e3", "ECONOMIST3"),
                UnlockConstraint.builder()
                        .addSumConstraint(24, Character.values())
                        .addSumConstraint(2, Character.ECONOMIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.ECONOMIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("e4", Setec2017Puzzle.create(
                Puzzle.create("e4", "ECONOMIST4"),
                UnlockConstraint.builder()
                        .addSumConstraint(24, Character.values())
                        .addSumConstraint(4, Character.ECONOMIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.ECONOMIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("e5", Setec2017Puzzle.create(
                Puzzle.create("e5", "ECONOMIST5"),
                UnlockConstraint.builder()
                        .addSumConstraint(24, Character.values())
                        .addSumConstraint(6, Character.ECONOMIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.ECONOMIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("e6", Setec2017Puzzle.create(
                Puzzle.create("e6", "ECONOMIST6"),
                UnlockConstraint.builder()
                        .addSumConstraint(24, Character.values())
                        .addSumConstraint(6, Character.ECONOMIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.ECONOMIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("e7", Setec2017Puzzle.create(
                Puzzle.create("e7", "ECONOMIST7"),
                UnlockConstraint.builder()
                        .addSumConstraint(24, Character.values())
                        .addSumConstraint(8, Character.ECONOMIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.ECONOMIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("e8", Setec2017Puzzle.create(
                Puzzle.create("e8", "ECONOMIST8"),
                UnlockConstraint.builder()
                        .addSumConstraint(24, Character.values())
                        .addSumConstraint(11, Character.ECONOMIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.ECONOMIST, 1)
                        .build()
        ));

        puzzlesBuilder.put("chemist", Setec2017Puzzle.create(
                Puzzle.create("chemist", "CHEMISTMETA"),
                UnlockConstraint.builder()
                        .addSumConstraint(40, Character.values())
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("ch1", Setec2017Puzzle.create(
                Puzzle.create("ch1", "CHEMIST1"),
                UnlockConstraint.builder()
                        .addSumConstraint(40, Character.values())
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CHEMIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("ch2", Setec2017Puzzle.create(
                Puzzle.create("ch2", "CHEMIST2"),
                UnlockConstraint.builder()
                        .addSumConstraint(40, Character.values())
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CHEMIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("ch3", Setec2017Puzzle.create(
                Puzzle.create("ch3", "CHEMIST3"),
                UnlockConstraint.builder()
                        .addSumConstraint(40, Character.values())
                        .addSumConstraint(3, Character.CHEMIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CHEMIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("ch4", Setec2017Puzzle.create(
                Puzzle.create("ch4", "CHEMIST4"),
                UnlockConstraint.builder()
                        .addSumConstraint(40, Character.values())
                        .addSumConstraint(5, Character.CHEMIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CHEMIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("ch5", Setec2017Puzzle.create(
                Puzzle.create("ch5", "CHEMIST5"),
                UnlockConstraint.builder()
                        .addSumConstraint(40, Character.values())
                        .addSumConstraint(7, Character.CHEMIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CHEMIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("ch6", Setec2017Puzzle.create(
                Puzzle.create("ch6", "CHEMIST6"),
                UnlockConstraint.builder()
                        .addSumConstraint(40, Character.values())
                        .addSumConstraint(7, Character.CHEMIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CHEMIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("ch7", Setec2017Puzzle.create(
                Puzzle.create("ch7", "CHEMIST7"),
                UnlockConstraint.builder()
                        .addSumConstraint(40, Character.values())
                        .addSumConstraint(9, Character.CHEMIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CHEMIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("ch8", Setec2017Puzzle.create(
                Puzzle.create("ch8", "CHEMIST8"),
                UnlockConstraint.builder()
                        .addSumConstraint(40, Character.values())
                        .addSumConstraint(11, Character.CHEMIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CHEMIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("ch9", Setec2017Puzzle.create(
                Puzzle.create("ch9", "CHEMIST9"),
                UnlockConstraint.builder()
                        .addSumConstraint(40, Character.values())
                        .addSumConstraint(14, Character.CHEMIST)
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.CHEMIST, 1)
                        .build()
        ));

        puzzlesBuilder.put("dynast", Setec2017Puzzle.create(
                Puzzle.create("dynast", "DYNASTMETA"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.values())
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.FIGHTER, 1)
                        .addCharacterLevels(Character.WIZARD, 1)
                        .addCharacterLevels(Character.CLERIC, 1)
                        .build()
        ));
        puzzlesBuilder.put("dynast1", Setec2017Puzzle.create(
                Puzzle.create("dynast1", "DYNAST1"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.values())
                        .addSumConstraint(2, Character.WIZARD)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dynast2", Setec2017Puzzle.create(
                Puzzle.create("dynast2", "DYNAST2"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.values())
                        .addSumConstraint(2, Character.FIGHTER)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dynast3", Setec2017Puzzle.create(
                Puzzle.create("dynast3", "DYNAST3"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.values())
                        .addSumConstraint(2, Character.CLERIC)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dynast4", Setec2017Puzzle.create(
                Puzzle.create("dynast4", "DYNAST4"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.values())
                        .addSumConstraint(3, Character.WIZARD)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dynast5", Setec2017Puzzle.create(
                Puzzle.create("dynast5", "DYNAST5"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.values())
                        .addSumConstraint(3, Character.FIGHTER)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dynast6", Setec2017Puzzle.create(
                Puzzle.create("dynast6", "DYNAST6"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.values())
                        .addSumConstraint(3, Character.CLERIC)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dynast7", Setec2017Puzzle.create(
                Puzzle.create("dynast7", "DYNAST7"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.values())
                        .addSumConstraint(4, Character.WIZARD)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dynast8", Setec2017Puzzle.create(
                Puzzle.create("dynast8", "DYNAST8"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.values())
                        .addSumConstraint(4, Character.FIGHTER)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dynast9", Setec2017Puzzle.create(
                Puzzle.create("dynast9", "DYNAST9"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.values())
                        .addSumConstraint(4, Character.CLERIC)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dynast10", Setec2017Puzzle.create(
                Puzzle.create("dynast10", "DYNAST10"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.values())
                        .addSumConstraint(5, Character.WIZARD)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dynast11", Setec2017Puzzle.create(
                Puzzle.create("dynast11", "DYNAST11"),
                UnlockConstraint.builder()
                        .addSumConstraint(6, Character.values())
                        .addSumConstraint(5, Character.FIGHTER)
                        .build(),
                SolveReward.builder().build()
        ));

        puzzlesBuilder.put("dungeon", Setec2017Puzzle.create(
                Puzzle.create("dungeon", "DUNGEONMETA"),
                UnlockConstraint.builder()
                        .addSumConstraint(18, Character.values())
                        .build(),
                SolveReward.builder()
                        .addCharacterLevels(Character.FIGHTER, 1)
                        .addCharacterLevels(Character.WIZARD, 1)
                        .addCharacterLevels(Character.CLERIC, 1)
                        .addCharacterLevels(Character.LINGUIST, 1)
                        .build()
        ));
        puzzlesBuilder.put("dungeon1", Setec2017Puzzle.create(
                Puzzle.create("dungeon1", "DUNGEON1"),
                UnlockConstraint.builder()
                        .addSumConstraint(18, Character.values())
                        .addSumConstraint(3, Character.WIZARD)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dungeon2", Setec2017Puzzle.create(
                Puzzle.create("dungeon2", "DUNGEON2"),
                UnlockConstraint.builder()
                        .addSumConstraint(18, Character.values())
                        .addSumConstraint(3, Character.FIGHTER)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dungeon3", Setec2017Puzzle.create(
                Puzzle.create("dungeon3", "DUNGEON3"),
                UnlockConstraint.builder()
                        .addSumConstraint(18, Character.values())
                        .addSumConstraint(3, Character.CLERIC)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dungeon4", Setec2017Puzzle.create(
                Puzzle.create("dungeon4", "DUNGEON4"),
                UnlockConstraint.builder()
                        .addSumConstraint(18, Character.values())
                        .addSumConstraint(2, Character.LINGUIST)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dungeon5", Setec2017Puzzle.create(
                Puzzle.create("dungeon5", "DUNGEON5"),
                UnlockConstraint.builder()
                        .addSumConstraint(18, Character.values())
                        .addSumConstraint(4, Character.WIZARD)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dungeon6", Setec2017Puzzle.create(
                Puzzle.create("dungeon6", "DUNGEON6"),
                UnlockConstraint.builder()
                        .addSumConstraint(18, Character.values())
                        .addSumConstraint(4, Character.FIGHTER)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dungeon7", Setec2017Puzzle.create(
                Puzzle.create("dungeon7", "DUNGEON7"),
                UnlockConstraint.builder()
                        .addSumConstraint(18, Character.values())
                        .addSumConstraint(4, Character.CLERIC)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dungeon8", Setec2017Puzzle.create(
                Puzzle.create("dungeon8", "DUNGEON8"),
                UnlockConstraint.builder()
                        .addSumConstraint(18, Character.values())
                        .addSumConstraint(3, Character.LINGUIST)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dungeon9", Setec2017Puzzle.create(
                Puzzle.create("dungeon9", "DUNGEON9"),
                UnlockConstraint.builder()
                        .addSumConstraint(18, Character.values())
                        .addSumConstraint(5, Character.CLERIC)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dungeon10", Setec2017Puzzle.create(
                Puzzle.create("dungeon10", "DUNGEON10"),
                UnlockConstraint.builder()
                        .addSumConstraint(18, Character.values())
                        .addSumConstraint(5, Character.FIGHTER)
                        .build(),
                SolveReward.builder().build()
        ));
        puzzlesBuilder.put("dungeon11", Setec2017Puzzle.create(
                Puzzle.create("dungeon11", "DUNGEON11"),
                UnlockConstraint.builder()
                        .addSumConstraint(18, Character.values())
                        .addSumConstraint(4, Character.LINGUIST)
                        .build(),
                SolveReward.builder().build()
        ));

        // TODO: add the rest of the rounds

        SolveReward.Builder solveRewardBuilder = SolveReward.builder();
        for (Character character : Character.values()) {
            solveRewardBuilder.addCharacterLevels(character, 1);
        }
        SolveReward oneLevelForEachCharacter = solveRewardBuilder.build();

        puzzlesBuilder.put("eventa", Setec2017Puzzle.create(
                Puzzle.create("eventa", "EVENTA"),
                UnlockConstraint.builder()
                        .setAutomaticUnlock(false)
                        .build(),
                oneLevelForEachCharacter
        ));
        puzzlesBuilder.put("eventb", Setec2017Puzzle.create(
                Puzzle.create("eventb", "EVENTB"),
                UnlockConstraint.builder()
                        .setAutomaticUnlock(false)
                        .build(),
                oneLevelForEachCharacter
        ));

        solveRewardBuilder = SolveReward.builder();
        for (Character character : Character.values()) {
            solveRewardBuilder.addCharacterLevels(character, 2);
        }
        SolveReward twoLevelsForEachCharacter = solveRewardBuilder.build();

        puzzlesBuilder.put("eventc", Setec2017Puzzle.create(
                Puzzle.create("eventc", "EVENTC"),
                UnlockConstraint.builder()
                        .setAutomaticUnlock(false)
                        .build(),
                twoLevelsForEachCharacter
        ));
        puzzlesBuilder.put("eventd", Setec2017Puzzle.create(
                Puzzle.create("eventd", "EVENTD"),
                UnlockConstraint.builder()
                        .setAutomaticUnlock(false)
                        .build(),
                twoLevelsForEachCharacter
        ));

        PUZZLES = puzzlesBuilder.build();
    }
}
