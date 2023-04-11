package org.main;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    @DisplayName(
            "Should throw an IllegalArgumentException when NODE_NAME_ARG is provided without a name")
    void getNodeNameWhenNodeNameArgIsProvidedWithoutNameThenThrowException() {
        String[] args = {Main.NODE_NAME_ARG};

        assertThrows(IllegalArgumentException.class, () -> Main.getNodeName(args));
    }

    @Test
    @DisplayName("Should return an empty map when no arguments are provided")
    void getNodeNameWhenNoArgsAreProvided() {
        String[] args = new String[0];

        Map<String, String> result = Main.getNodeName(args);

        assertTrue(
                result.isEmpty(), "The result map should be empty when no arguments are provided");
    }

    @Test
    @DisplayName("Should return a map with node name when NODE_NAME_ARG is provided")
    void getNodeNameWhenNodeNameArgIsProvided() {
        String[] args = {Main.NODE_NAME_ARG, "testNode"};
        Map<String, String> result = Main.getNodeName(args);

        assertEquals(1, result.size());
        assertTrue(result.containsKey(Main.NODE_NAME_ARG));
        assertEquals("testNode", result.get(Main.NODE_NAME_ARG));
    }

    @Test
    @DisplayName("Should return a map with GENESIS_GENERATOR_ARG when it is provided")
    void getNodeNameWhenGenesisGeneratorArgIsProvided() {
        String[] args = {Main.NODE_NAME_ARG, "node1", Main.GENESIS_GENERATOR_ARG};
        Map<String, String> result = Main.getNodeName(args);

        assertTrue(result.containsKey(Main.GENESIS_GENERATOR_ARG));
        assertEquals("node1", result.get(Main.NODE_NAME_ARG));
    }

    @Test
    @DisplayName(
            "Should return a map with both NODE_NAME_ARG and GENESIS_GENERATOR_ARG when both are provided")
    void getNodeNameWhenBothArgsAreProvided() {
        String[] args = {Main.NODE_NAME_ARG, "testNode", Main.GENESIS_GENERATOR_ARG};
        Map<String, String> result = Main.getNodeName(args);

        assertEquals(2, result.size(), "The result map should contain both arguments");
        assertEquals(
                "testNode", result.get(Main.NODE_NAME_ARG), "The node name should be 'testNode'");
        assertEquals(
                "stub",
                result.get(Main.GENESIS_GENERATOR_ARG),
                "The genesis generator argument should be present");
    }
}