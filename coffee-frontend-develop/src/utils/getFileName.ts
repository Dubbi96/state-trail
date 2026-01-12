export const getFileName = (url: string | null) => {
    if (!url) return null;
    const decodedUrl = decodeURIComponent(url);

    // 정규식으로 __uuid__ 앞의 이름 추출
    const match = decodedUrl.match(/\/([^\/]+?)__uuid__([a-f0-9-]+)\.([a-zA-Z0-9]+)$/);

    if (!match) return null;
    const name = match[1]; // 예: 드로잉 2
    const ext = match[3]; // 예: abcxyz

    // ✅ 다시 join
    const filename = `${name}.${ext}`;
    return filename;
};
