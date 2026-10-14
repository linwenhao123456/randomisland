// 读取 version.json 动态填充页面（每次部署都会更新该文件，实现“实时更新”）
async function loadMeta() {
  try {
    const res = await fetch('version.json', { cache: 'no-store' });
    if (!res.ok) throw new Error('version.json ' + res.status);
    const d = await res.json();

    const set = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = val; };

    set('ver', 'v' + d.version);
    set('ver2', d.version);
    set('mc', (d.mcVersion || '1.20.1') + ' · ' + (d.loader || 'Fabric'));
    set('size', d.size || '—');
    set('date', d.updated || '—');
    set('file', d.fileName || 'random_island.jar');
    set('file2', d.fileName || 'random_island.jar');
    set('clDate', '更新于 ' + (d.updated || '—') + ' · v' + d.version);
    set('changelog', d.changelog || '暂无更新说明。');

    document.title = (d.name || '随机空岛') + ' ' + d.version;

    const dl = document.getElementById('dl');
    const nav = document.getElementById('navDownload');
    if (dl && d.fileName) dl.href = 'download/' + d.fileName;
    if (nav && d.fileName) nav.href = 'download/' + d.fileName;
  } catch (e) {
    console.error('加载版本信息失败：', e);
  }
}

loadMeta();
