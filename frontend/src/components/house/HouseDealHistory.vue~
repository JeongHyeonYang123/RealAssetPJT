<script setup>

</script>

<template>
  <form action="${root }/search/searchForm" method="post">
    <div class="wrapper">
      <div class="container search-container">
        <div class="text-center search-box">
          <input type="hidden" name="action" value="searchForm" />
          <div class="row justify-content-end">

            <input type="hidden" name="action" value="searchForm" />
            <div class="col-md-2">
              <select class="form-select" id="sido" name="sido" required>
                <option value="" selected disabled>시도 선택</option>
              </select>
            </div>
            <div class="col-md-2">
              <select class="form-select" id="gugun" name="gugun" required>
                <option value="" selected disabled>구군 선택</option>
              </select>
            </div>
            <div class="col-md-2">
              <select class="form-select" id="dong" name="dong" required>
                <option value="" selected disabled>읍면동 선택</option>
              </select>
            </div>
            <div class="col-md-2 text-center" style="width: 100px">
              <input type="submit" class="btn btn-primary" id="btn_apt_search"
                     value="검색">

            </div>
          </div>
        </div>
      </div>

      <div id="aside">
        <h4>상세 정보</h4>
        <c:forEach items="${ houseList}" var="item">
          <strong>${item.aptNm}</strong>
          <br>
          <span style="color: #6c757d">(${item.umdName},
						${item.buildeYear}년)</span>
          <br>
          <span style="font-size: 0.9em;">전용면적: ${item.area}㎡ | 층수:
						${item.floor}층</span>
          <br>
          거래금액: <span style="color: #007bff; font-weight: bold;">${item.dealAmount}만원</span>
          <hr>
        </c:forEach>
        <nav class="d-flex justify-content-center">
          <ul class="pagination">
            <!-- 이전 버튼 -->
            <c:if test="${page.hasPrev}">
              <li class="page-item"><a class="page-link" href="#"
                                       data-page="${page.startPage - 1}">이전</a></li>
            </c:if>

            <!-- 페이지 번호 -->
            <c:forEach begin="${page.startPage}" end="${page.endPage}"
                       var="item">
              <li class="page-item ${page.currentPage == item ? 'active' : ''}">
                <a class="page-link" href="#" data-page="${item}">${item}</a>
              </li>
            </c:forEach>

            <!-- 다음 버튼 -->
            <c:if test="${page.hasNext}">
              <li class="page-item"><a class="page-link" href="#"
                                       data-page="${page.endPage + 1}">다음</a></li>
            </c:if>
          </ul>
        </nav>
      </div>
      <div id="map"></div>
    </div>
  </form>
</template>

<style scoped>

</style>