import { Cookies } from 'react-cookie';

const cookies = new Cookies();

const setCookies = (key: string, value: string) => {
    return cookies.set(key, value);
};

const getCookies = (key: string) => {
    return cookies.get(key);
};

const removeCookies = (key: string) => {
    return cookies.remove(key, { path: '/' });
};

export { getCookies, setCookies, removeCookies };
