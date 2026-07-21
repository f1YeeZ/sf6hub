CREATE TABLE IF NOT EXISTS carousel_slides (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    subtitle VARCHAR(300) NOT NULL DEFAULT '',
    tag VARCHAR(40) NOT NULL DEFAULT '',
    color VARCHAR(40) NOT NULL DEFAULT '#9040d0',
    bg TEXT NOT NULL,
    image_url VARCHAR(500) NOT NULL DEFAULT '',
    link_url VARCHAR(500) NOT NULL DEFAULT '',
    display_order INTEGER NOT NULL DEFAULT 0,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(40) NOT NULL DEFAULT '',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_carousel_slides_published_order ON carousel_slides(published, display_order, id);

INSERT INTO carousel_slides (title, subtitle, tag, color, bg, display_order, published, created_by)
SELECT 'Season 3 平衡调整上线', '新版本改动、角色强化与系统调整', '版本更新', '#9040d0',
       'linear-gradient(135deg, rgba(100,30,180,0.3) 0%, rgba(20,10,40,0.8) 60%, #0c0e12 100%)',
       10, TRUE, 'system'
WHERE NOT EXISTS (SELECT 1 FROM carousel_slides);

INSERT INTO carousel_slides (title, subtitle, tag, color, bg, display_order, published, created_by)
SELECT 'EVO 2026 赛事报名', '拉斯维加斯 · 7月31日-8月2日 · 奖金池 $100,000', '赛事', '#c080f0',
       'linear-gradient(135deg, rgba(180,80,240,0.3) 0%, rgba(20,10,40,0.8) 60%, #0c0e12 100%)',
       20, TRUE, 'system'
WHERE NOT EXISTS (SELECT 1 FROM carousel_slides WHERE title = 'EVO 2026 赛事报名');

INSERT INTO carousel_slides (title, subtitle, tag, color, bg, display_order, published, created_by)
SELECT '连招实验室开放', '记录训练路线 · 分享进阶心得', '攻略', '#60c0f0',
       'linear-gradient(135deg, rgba(60,120,220,0.3) 0%, rgba(10,20,40,0.8) 60%, #0c0e12 100%)',
       30, TRUE, 'system'
WHERE NOT EXISTS (SELECT 1 FROM carousel_slides WHERE title = '连招实验室开放');

INSERT INTO carousel_slides (title, subtitle, tag, color, bg, display_order, published, created_by)
SELECT 'CPT 2026 巡回赛日程', '线上预选 · 线下决赛 · 积分排名更新', '赛事', '#f0a060',
       'linear-gradient(135deg, rgba(200,120,60,0.3) 0%, rgba(40,20,10,0.8) 60%, #0c0e12 100%)',
       40, TRUE, 'system'
WHERE NOT EXISTS (SELECT 1 FROM carousel_slides WHERE title = 'CPT 2026 巡回赛日程');
