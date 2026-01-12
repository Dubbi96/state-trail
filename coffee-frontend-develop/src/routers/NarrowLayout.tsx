
import { Stack } from '@mui/material';

// maxWidth : md만큼 늘어나고 나머지는 가운데 정렬
const MAX_WIDTH = 900;

const NarrowLayout = ({ children }: { children: React.ReactNode }) => {

    return (
        <Stack sx={{ flex: 1, width: '100%', maxWidth: MAX_WIDTH, margin: '0 auto'}}>
            {children}
        </Stack>
    );
};

export default NarrowLayout;
