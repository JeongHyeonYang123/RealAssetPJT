//package com.ssafy.home.util;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class BoardSearchHelper {
//
//    // 제목 기반 검색
//    public static List<Board> searchByTitle(List<Board> boards, String keyword) {
//        List<Board> result = new ArrayList<>();
//        for (Board dto : boards) {
//            if (dto.getTitle() != null && dto.getTitle().contains(keyword)) {
//                result.add(dto);
//            }
//        }
//        return result;
//    }
//
//    // 내용 기반 검색
//    public static List<Board> searchByContent(List<Board> boards, String keyword) {
//        List<Board> result = new ArrayList<>();
//        for (Board dto : boards) {
//            if (dto.getContent() != null && dto.getContent().contains(keyword)) {
//                result.add(dto);
//            }
//        }
//        return result;
//    }
//
//    // KMP 기반 결과 내 재검색 (내용에서만)
//    public static List<Board> searchWithinContent(List<Board> filteredBoards, String subKeyword) {
//        List<Board> result = new ArrayList<>();
//        for (Board dto : filteredBoards) {
//            if (dto.getContent() != null && !SearchUtil.kmpSearch(dto.getContent(), subKeyword).isEmpty()) {
//                result.add(dto);
//            }
//        }
//        return result;
//    }
//}