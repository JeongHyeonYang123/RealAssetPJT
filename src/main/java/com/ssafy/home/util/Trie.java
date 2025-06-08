package com.ssafy.home.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Trie {
    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEndOfWord = false;
    }

    private final TrieNode root = new TrieNode();

    public void insert(String word) {
        TrieNode node = root;
        for (char ch : word.toCharArray()) {
            node = node.children.computeIfAbsent(ch, c -> new TrieNode());
        }
        node.isEndOfWord = true;
    }

    public List<String> searchPrefix(String prefix) {
        List<String> result = new ArrayList<>();
        TrieNode node = root;
        for (char ch : prefix.toCharArray()) {
            if (!node.children.containsKey(ch)) return result;
            node = node.children.get(ch);
        }
        dfs(node, new StringBuilder(prefix), result);
        return result;
    }

    private void dfs(TrieNode node, StringBuilder path, List<String> result) {
        if (node.isEndOfWord) result.add(path.toString());
        for (char ch : node.children.keySet()) {
            path.append(ch);
            dfs(node.children.get(ch), path, result);
            path.deleteCharAt(path.length() - 1);
        }
    }
}
