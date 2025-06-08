


<template>
  <div class="news-view">
    <div class="container">
      <h1>부동산 뉴스</h1>

      <div class="news-grid">
        <article class="news-card" v-for="news in newsItems" :key="news.id"
                 @click="goToNews(news.link)"
                 style="cursor:pointer"
        >

          <div class="news-image">
            <img :src="news.image" :alt="news.title" />
          </div>
          <div class="news-content">
            <div class="news-meta">
              <span class="news-source">{{ news.source }}</span>
              <span class="news-date">{{ news.date }}</span>
            </div>
            <h2>{{ news.title }}</h2>
            <p>{{ news.excerpt }}</p>
          </div>
        </article>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const newsItems = ref([])

onMounted(async () => {
  const res = await fetch('http://localhost:5000/news?query=부동산&display=8')
  const data = await res.json()
  newsItems.value = data.map((item, idx) => ({
    id: idx + 1,
    title: item.title.replace(/<[^>]*>/g, ''),
    excerpt: item.description.replace(/<[^>]*>/g, ''),
    image: item.thumbnail || 'https://via.placeholder.com/400x200?text=No+Image',
    source: item.originallink ? new URL(item.originallink).hostname : '네이버뉴스',
    date: item.pubDate ? item.pubDate.split(' ')[0].replace(/-/g, '.') : '',
    link: item.link || item.originallink, // 반드시 둘 중 하나를 사용

  }))
})


function goToNews(link: string) {
  console.log('이동할 링크:', link)
  window.open(link, '_blank')
}
</script>


<style lang="scss" scoped>
.news-view {
  padding: var(--space-4) 0;
  
  h1 {
    margin-bottom: var(--space-4);
  }
}

.news-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--space-3);
}

.news-card {
  background: var(--color-white);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
  transition: transform var(--transition-fast);
  cursor: pointer;
  
  &:hover {
    transform: translateY(-4px);
  }
}

.news-image {
  height: 200px;
  overflow: hidden;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform var(--transition-normal);
  }
  
  .news-card:hover & img {
    transform: scale(1.05);
  }
}

.news-content {
  padding: var(--space-3);
}

.news-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-2);
  font-size: 0.875rem;
  
  .news-source {
    color: var(--color-primary);
    font-weight: 500;
  }
  
  .news-date {
    color: var(--color-gray-600);
  }
}

h2 {
  font-size: 1.25rem;
  margin-bottom: var(--space-2);
  line-height: 1.4;
}

p {
  color: var(--color-gray-700);
  font-size: 0.875rem;
  line-height: 1.6;
}
</style>