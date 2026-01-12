import { Color, PaletteOptions } from '@mui/material';

type CustomColor = Omit<Color, 'A100' | 'A200' | 'A400' | 'A700'> & {
    main: string;
};

type CustomPaletteOptions = PaletteOptions & {
    blue: CustomColor;
    grey: CustomColor;
    success: CustomColor;
    red: CustomColor;
    yellow: CustomColor;
    divider: string;
    text: { primary: string };
    common: { black: string; white: string };
    action: { hover: string; focus: string; selected: string };
};

export const palette: CustomPaletteOptions = {
    blue: {
        50: '#eff6ff',
        100: '#dbeafe',
        200: '#bfdbfe',
        300: '#a3cfff',
        400: '#60a5fa',
        500: '#3b82f6',
        600: '#2563eb',
        700: '#173da6',
        800: '#1a3478',
        900: '#14204a',

        // optional
        main: '#3b82f6', // = 500
    },
    grey: {
        50: '#fafafa',
        100: '#f4f4f5',
        200: '#e4e4e7',
        300: '#d4d4d8',
        400: '#a1a1aa',
        500: '#71717a',
        600: '#52525b',
        700: '#3f3f46',
        800: '#27272a',
        900: '#18181b',
        // optional (MUI에서 main 쓰는 경우 대비)
        main: '#71717a', // = 500
    },
    success: {
        main: '#0E9F6E', // 500
        50: '#F3FAF7',
        100: '#DEF7EC',
        200: '#BCF0DA',
        300: '#84E1BC',
        400: '#31C48D',
        500: '#0E9F6E',
        600: '#057A55',
        700: '#046C4E',
        800: '#03543F',
        900: '#014737',
    },
    red: {
        main: '#F05252', // 500
        50: '#FDF2F2',
        100: '#FDE8E8',
        200: '#FBD5D5',
        300: '#F8B4B4',
        400: '#F98080',
        500: '#F05252',
        600: '#E02424',
        700: '#C81E1E',
        800: '#9B1C1C',
        900: '#771D1D',
    },
    yellow: {
        main: '#ffeb3b', // 500
        50: '#fffde7',
        100: '#fff9c4',
        200: '#fff59d',
        300: '#fff176',
        400: '#ffee58',
        500: '#ffeb3b',
        600: '#fdd835',
        700: '#fbc02d',
        800: '#f9a825',
        900: '#f57f17',
    },
    divider: '#E5E7EB',
    text: { primary: '#111928' },
    common: { black: '#000000', white: '#FFFFFF' },
    action: { hover: '#0000000A', focus: '#0000000A', selected: '#EBF5FF' },
};
