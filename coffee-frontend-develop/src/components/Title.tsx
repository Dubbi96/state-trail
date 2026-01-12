import { palette } from '@/themes';
import { Stack, Typography } from '@mui/material';

type TitleProps = {
    title: string;
    children?: React.ReactNode;
};

const Title = ({ title, children }: TitleProps) => {
    return (
        <Stack
            sx={{
                flexDirection: 'row',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '24px 32px',
                width: '100%',
            }}
        >
            <Typography sx={{ color: palette.grey[800], fontSize: '28px', fontWeight: '600' }}>{title}</Typography>
            <Stack gap={'10px'} direction={'row'} sx={{ height: '40px' }}>
                {children}
            </Stack>
        </Stack>
    );
};

export default Title;
